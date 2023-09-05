package com.dqv5.autogennum.service.impl;

import com.dqv5.autogennum.entity.AutoNumberRecord;
import com.dqv5.autogennum.entity.AutoNumberRule;
import com.dqv5.autogennum.exception.GenerateNumberException;
import com.dqv5.autogennum.repository.AutoNumberRecordRepository;
import com.dqv5.autogennum.repository.AutoNumberRuleRepository;
import com.dqv5.autogennum.service.AutoNumberService;
import com.dqv5.autogennum.utils.RedisTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author duqian
 * @date 2021/5/31
 */
@Slf4j
@Service
public class AutoNumberServiceImpl implements AutoNumberService {
    @Resource
    private AutoNumberRuleRepository autoNumberRuleRepository;
    @Resource
    private AutoNumberRecordRepository autoNumberRecordRepository;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String LOCK_KEY = "AUTO_NUMBER_LOCK";


    @Override
    public List<AutoNumberRule> queryAllList() {
        Sort sort = Sort.by(Sort.Direction.ASC, "lastModifiedDate");
        return autoNumberRuleRepository.findAll(sort);
    }

    @Override
    public void insert(AutoNumberRule autoNumberRule) {
        Date now = new Date();
        autoNumberRule.setCreateDate(now);
        autoNumberRule.setLastModifiedDate(now);
        autoNumberRuleRepository.save(autoNumberRule);
    }

    @Override
    public void update(AutoNumberRule autoNumberRule) {
        int ruleId = autoNumberRule.getRuleId();
        AutoNumberRule rule = autoNumberRuleRepository.findById(ruleId).orElseThrow(() -> new GenerateNumberException("未查到ruleId:" + ruleId));
        Date now = new Date();
        rule.setRuleName(autoNumberRule.getRuleName());
        rule.setExpression(autoNumberRule.getExpression());
        rule.setRemark(autoNumberRule.getRemark());
        rule.setLastModifiedDate(now);
        autoNumberRuleRepository.save(rule);
    }

    @Override
    public void delete(int ruleId) {
        if (!autoNumberRuleRepository.existsById(ruleId)) {
            throw new GenerateNumberException("未查到ruleId:" + ruleId);
        }
        autoNumberRuleRepository.deleteById(ruleId);
    }

    /**
     * 根据规则id和流程标识生成一个编号
     * 支持变量：${YYYY} ${YY} ${MM} ${DD} ${S1}~${S9}
     *
     * @param ruleId
     * @return
     */
    @Override
    public String generateNextNumber(int ruleId) {
        AutoNumberRule rule = autoNumberRuleRepository.findById(ruleId).orElseThrow(() -> new GenerateNumberException("未查到ruleId:" + ruleId));
        // 规则表达式，例如：DQ-${YYYY}-${MM}-${S3}
        final String expression = rule.getExpression();
        // 取出最后一次生成的编号，例如：DQ-2021-05-003
        String latestNumber = rule.getLatestNumber();
        // 生成下一个流水序列号
        int serialNumber = this.getSerialNumber(expression, latestNumber);
        // 替换流水号变量，数字前补0
        String result = this.replaceYmdVars(expression);
        for (int i = 1; i <= 9; i++) {
            result = result.replace("${S" + i + "}", String.format("%0" + i + "d", serialNumber));
        }
        rule.setLatestNumber(result);
        rule.setLastModifiedDate(new Date());
        autoNumberRuleRepository.save(rule);

        AutoNumberRecord autoNumberRecord = new AutoNumberRecord();
        autoNumberRecord.setRecordNumber(result);
        autoNumberRecord.setCreateDate(new Date());
        autoNumberRecord.setLastModifiedDate(new Date());
        autoNumberRecordRepository.save(autoNumberRecord);
        return result;
    }

    @Override
    public String safeGenerateNextNumber(int ruleId) {
        String requestId = UUID.randomUUID().toString();
        while (true) {
            boolean getLockOk = RedisTool.tryGetDistributedLock(stringRedisTemplate, LOCK_KEY, requestId, 30);
            if (getLockOk) {
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            AutoNumberRule rule = autoNumberRuleRepository.findById(ruleId).orElseThrow(() -> new GenerateNumberException("未查到ruleId:" + ruleId));
            // 规则表达式，例如：DQ-${YYYY}-${MM}-${S3}
            final String expression = rule.getExpression();
            // 取出最后一次生成的编号，例如：DQ-2021-05-003
            String latestNumber = rule.getLatestNumber();
            // 生成下一个流水序列号
            int serialNumber = this.getSerialNumber(expression, latestNumber);
            // 替换流水号变量，数字前补0
            String result = this.replaceYmdVars(expression);
            for (int i = 1; i <= 9; i++) {
                result = result.replace("${S" + i + "}", String.format("%0" + i + "d", serialNumber));
            }
            rule.setLatestNumber(result);
            rule.setLastModifiedDate(new Date());
            autoNumberRuleRepository.save(rule);

            AutoNumberRecord autoNumberRecord = new AutoNumberRecord();
            autoNumberRecord.setRecordNumber(result);
            autoNumberRecord.setCreateDate(new Date());
            autoNumberRecord.setLastModifiedDate(new Date());
            autoNumberRecordRepository.save(autoNumberRecord);
            return result;
        } finally {
            RedisTool.releaseDistributedLock(stringRedisTemplate, LOCK_KEY, requestId);
        }
    }

    @Override
    public List<AutoNumberRecord> queryRecordList() {
        return autoNumberRecordRepository.findByOrderByRecordNumberDesc();
    }

    /**
     * @param expression   规则表达式，例如：DQ-${YYYY}-${MM}-${S3}
     * @param latestNumber 最后一次生成的编号，例如：DQ-2021-05-003
     * @return
     */
    private int getSerialNumber(final String expression, final String latestNumber) {
        if (StringUtils.isBlank(latestNumber)) {
            return 1;
        }
        // DQ-${YYYY}-${MM}-${S([0-9])}
        String exp2 = this.formatYmdRex(expression);
        exp2 = exp2.replaceAll("\\$\\{S[0-9]}", "\\\\\\$\\\\{S([0-9])}");
        log.debug("exp2:{}", exp2);
        Matcher matcher = Pattern.compile(exp2).matcher(expression);
        if (!matcher.find()) {
            log.error("未找到");
            return 1;
        }
        int serialNumberLength = Integer.valueOf(matcher.group(1));
        log.debug("serialNumberLength: {}", serialNumberLength);

        String exp = this.replaceYmdVars(expression);
        exp = exp.replaceAll("\\$\\{S[0-9]}", "([0-9]{" + serialNumberLength + "})");
        log.debug("exp:{}", exp);
        matcher = Pattern.compile(exp).matcher(latestNumber);
        if (!matcher.find()) {
            return 1;
        }
        // n位数的最大值，如999
        StringBuilder max = new StringBuilder();
        for (int i = 0; i < serialNumberLength; i++) {
            max.append(9);
        }
        String group = matcher.group(1);
        if (group.equals(max.toString())) {
            throw new GenerateNumberException("编号已达到本阶段最大值：" + latestNumber);
        }
        // 未达到最大值，加1后返回
        int value = Integer.valueOf(group);
        return value + 1;
    }

    /**
     * 替换年月日变量
     *
     * @return
     */
    private String replaceYmdVars(final String expression) {
        LocalDate localDate = LocalDate.now();
        String YYYY = localDate.getYear() + "";
        String YY = localDate.getYear() - 2000 + "";
        String MM = localDate.getMonthValue() < 10 ? ("0" + localDate.getMonthValue()) : ("" + localDate.getMonthValue());
        String DD = localDate.getDayOfMonth() < 10 ? ("0" + localDate.getDayOfMonth()) : ("" + localDate.getDayOfMonth());
        String result = expression;
        String[] keys = {"${YYYY}", "${YY}", "${MM}", "${DD}"};
        String[] values = {YYYY, YY, MM, DD};
        for (int i = 0; i < keys.length; i++) {
            result = result.replace(keys[i], values[i]);
        }
        return result;
    }

    /**
     * 将表达式中的${YYYY} 替换为 \$\{YYYY}
     *
     * @param expression
     * @return
     */
    private String formatYmdRex(final String expression) {
        String result = expression;
        String[] keys = {"${YYYY}", "${YY}", "${MM}", "${DD}"};
        String[] values = {"\\$\\{YYYY}", "\\$\\{YY}", "\\$\\{MM}", "\\$\\{DD}"};
        for (int i = 0; i < keys.length; i++) {
            result = result.replace(keys[i], values[i]);
        }
        return result;
    }


    public static void main(String[] args) {
        AutoNumberServiceImpl service = new AutoNumberServiceImpl();
        // 规则表达式，例如：DQ${YYYY}${MM}${DD}${S3}
        final String expression = "DQ${YYYY}${MM}${DD}${S3}";
        // 取出最后一次生成的编号，例如：DQ20230901003
        String latestNumber = "DQ20230901003";
        // 生成下一个流水序列号
        int serialNumber = service.getSerialNumber(expression, latestNumber);
        // 替换流水号变量，数字前补0
        String result = service.replaceYmdVars(expression);
        for (int i = 1; i <= 9; i++) {
            result = result.replace("${S" + i + "}", String.format("%0" + i + "d", serialNumber));
        }
        log.info("next:{}", result);
    }
}

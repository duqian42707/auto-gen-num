package com.dqv5.autogennum.config;

import com.dqv5.autogennum.entity.AutoNumberRule;
import com.dqv5.autogennum.service.AutoNumberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class CommandLineRunnerImpl implements CommandLineRunner {
    @Resource
    private AutoNumberService autoNumberService;
    @Value("${autogennum.init-rule}")
    private String initRuleExpression;

    @Override
    public void run(String... args) throws Exception {
        log.info("初始化规则");
        AutoNumberRule rule = new AutoNumberRule();
        rule.setRuleId(1);
        rule.setExpression(initRuleExpression);
        rule.setRuleName("默认规则");
        autoNumberService.insert(rule);
        log.info("执行结束");
    }
}

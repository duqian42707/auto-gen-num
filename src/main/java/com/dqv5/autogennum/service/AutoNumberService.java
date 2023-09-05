package com.dqv5.autogennum.service;

import com.dqv5.autogennum.entity.AutoNumberRecord;
import com.dqv5.autogennum.entity.AutoNumberRule;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author duqian
 * @date 2021/5/31
 */
public interface AutoNumberService {

    List<AutoNumberRule> queryAllList();

    void insert(AutoNumberRule autoNumberRule);

    void update(AutoNumberRule autoNumberRule);

    void delete(int ruleId);

    String generateNextNumber(int ruleId);

    String safeGenerateNextNumber(int ruleId);

    List<AutoNumberRecord> queryRecordList();
}

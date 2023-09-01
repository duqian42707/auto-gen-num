package com.dqv5.autogennum.repository;

import com.dqv5.autogennum.entity.AutoNumberRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author duqian
 * @date 2021/5/31
 */
public interface AutoNumberRuleRepository extends JpaRepository<AutoNumberRule, Integer>, JpaSpecificationExecutor<AutoNumberRule> {
}

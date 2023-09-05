package com.dqv5.autogennum.repository;

import com.dqv5.autogennum.entity.AutoNumberRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * @author duqian
 * @date 2021/5/31
 */
public interface AutoNumberRecordRepository extends JpaRepository<AutoNumberRecord, String>, JpaSpecificationExecutor<AutoNumberRecord> {
    List<AutoNumberRecord> findByOrderByRecordNumberDesc();
}

package com.dqv5.autogennum.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "ACT_AUTONUM_RULE")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AutoNumberRule implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private Integer ruleId;
    private String ruleName;
    private String expression;
    private String remark;
    private String latestNumber;
    private Date createDate;
    private Date lastModifiedDate;

}

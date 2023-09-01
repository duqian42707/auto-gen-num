package com.dqv5.autogennum.common.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * TODO:TODO
 * Auther:徐成
 * Date:2018/1/31
 * Email:old_camel@126.com
 */
@Data
@Builder
public class ReturnEntity<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private String message;
    private T data;
    private String errorMessage;
}

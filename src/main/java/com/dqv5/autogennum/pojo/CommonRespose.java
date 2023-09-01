package com.dqv5.autogennum.pojo;

import lombok.Builder;
import lombok.Data;

/**
 * @author duq
 * @date 2021/12/14
 */
@Data
@Builder
public class CommonRespose {
    private boolean success;
    private String message;
    private Object data;
}

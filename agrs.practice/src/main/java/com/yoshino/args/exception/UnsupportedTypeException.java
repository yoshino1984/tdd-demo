package com.yoshino.args.exception;

import lombok.Getter;

/**
 * @author xiaoyi
 * 2022/12/11 14:30
 * @since
 **/
@Getter
public class UnsupportedTypeException extends RuntimeException{

    private Class<?> type;

    public UnsupportedTypeException(Class<?> type) {
        this.type = type;
    }
}

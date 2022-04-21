package com.yoshino.args;

/**
 * @author xiaoyi
 * 2022/4/22 01:18
 * @since
 **/
public class UnsupportedOptionTypeException extends RuntimeException {
    private final String value;
    private final Class<?> type;

    public UnsupportedOptionTypeException(String value, Class<?> type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }
}

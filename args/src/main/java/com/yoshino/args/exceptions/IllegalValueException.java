package com.yoshino.args.exceptions;

/**
 * @author xiaoyi
 * 2022/4/22 01:13
 * @since
 **/
public class IllegalValueException extends RuntimeException {

    private final String option;
    private String value;

    public IllegalValueException(String option, String value) {
        this.option = option;
        this.value = value;
    }

    public String getOption() {
        return option;
    }

    public String getValue() {
        return value;
    }
}

package com.yoshino.args.exception;

/**
 * @author xiaoyi
 * 2022/12/11 13:33
 * @since
 **/
public class IllegalValueException extends RuntimeException {
    private String option;
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

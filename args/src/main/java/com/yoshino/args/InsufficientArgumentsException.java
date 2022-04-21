package com.yoshino.args;

/**
 * @author xiaoyi
 * 2022/4/20 23:20
 * @since
 **/
public class InsufficientArgumentsException extends RuntimeException {

    private final String option;

    public InsufficientArgumentsException(String option) {
        this.option = option;
    }


    public String getOption() {
        return option;
    }
}

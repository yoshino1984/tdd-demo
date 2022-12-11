package com.yoshino.args.exception;

/**
 * @author xiaoyi
 * 2022/12/11 13:02
 * @since
 **/
public class InsufficientArgumentsException extends RuntimeException {

    private String option;

    public InsufficientArgumentsException(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }

}

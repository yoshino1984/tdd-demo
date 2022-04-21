package com.yoshino.args;

/**
 * @author xiaoyi
 * 2022/4/21 00:11
 * @since
 **/
public class IllegalOptionException extends RuntimeException {

    private final String parameter;

    public IllegalOptionException(String option) {
        this.parameter = option;
    }


    public String getParameter() {
        return parameter;
    }
}

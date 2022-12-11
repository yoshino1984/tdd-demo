package com.yoshino.args.exception;

import lombok.Getter;

/**
 * @author xiaoyi
 * 2022/12/10 19:17
 * @since
 **/
@Getter
public class TooManyArgumentsException extends RuntimeException {

    private String option;

    public TooManyArgumentsException(String option) {
        this.option = option;
    }
}

package com.yoshino.args;

import java.util.List;

/**
 * @author xiaoyi
 * 2022/12/10 19:01
 * @since
 **/
interface OptionParser<T> {
    T parseValue(List<String> arguments, Option option);
}

package com.yoshino.args;

import java.util.List;

/**
 * @author xiaoyi
 * 2022/4/19 23:53
 * @since
 **/
interface OptionParser {
    public Object parse(List<String> arguments, Option option);
}

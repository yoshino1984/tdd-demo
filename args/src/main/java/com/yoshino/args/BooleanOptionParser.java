package com.yoshino.args;

import java.util.List;

/**
 * @author xiaoyi
 * 2022/4/19 23:53
 * @since
 **/
class BooleanOptionParser implements OptionParser<Boolean> {

    @Override
    public Boolean parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        if (index + 1 < arguments.size()
            && !arguments.get(index + 1).startsWith("-")) {
            throw new TooManyArgumentsException(option.value());
        }

        return index != -1;
    }
}

package com.yoshino.args;

import java.util.List;
import java.util.function.Function;

/**
 * @author xiaoyi
 * 2022/4/19 23:53
 * @since
 **/
class SingleOptionParser<T> implements OptionParser {
    Function<String, T> valueParser;

    public SingleOptionParser(Function<String, T> valueParser) {
        this.valueParser = valueParser;
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        String value = arguments.get(index + 1);
        return valueParser.apply(value);
    }

}

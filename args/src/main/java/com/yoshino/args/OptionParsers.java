package com.yoshino.args;

import com.yoshino.args.exceptions.IllegalValueException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * @author xiaoyi
 * 2022/4/19 23:53
 * @since
 **/
class OptionParsers {

    public static OptionParser<Boolean> bool() {
        return (arguments, option) ->
            values(arguments, option, 0).map(it -> true).orElse(false);
    }

    public static <T> OptionParser<T> unary(T defaultValue, Function<String, T> valueParser) {
        return (arguments, option) -> values(arguments, option, 1)
            .map(it -> parseValue(option, it.get(0), valueParser))
            .orElse(defaultValue);
    }

    public static <T> OptionParser<T[]> list(IntFunction<T[]> generator, Function<String, T> valueParser) {
        return (arguments, option) -> values(arguments, option)
            .map(it -> it.stream().map(value -> parseValue(option, value, valueParser))
                .toArray(generator))
            .orElse(generator.apply(0));
    }


    static Optional<List<String>> values(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        return Optional.ofNullable(index == -1 ? null : values(arguments, index));
    }

    static Optional<List<String>> values(List<String> arguments, Option option, int exceptedSize) {
        return values(arguments, option).map(it -> checkSizeAndGet(option, exceptedSize, it));

    }

    private static List<String> checkSizeAndGet(Option option, int exceptedSize, List<String> values) {
        if (values.size() < exceptedSize) {
            throw new InsufficientArgumentsException(option.value());
        }
        if (values.size() > exceptedSize) {
            throw new TooManyArgumentsException(option.value());
        }
        return values;
    }

    private static <T> T parseValue(Option option, String value, Function<String, T> valueParser) {
        try {
            return valueParser.apply(value);
        } catch (Exception e) {
            throw new IllegalValueException(option.value(), value);
        }
    }

    private static List<String> values(List<String> arguments, int index) {
        return arguments.subList(index + 1, IntStream.range(index + 1, arguments.size())
            .filter(it -> arguments.get(it).matches("^-[a-zA-Z]+$"))
            .findFirst().orElse(arguments.size()));
    }

}

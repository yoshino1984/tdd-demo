package com.yoshino.args;

import com.yoshino.args.exception.IllegalValueException;
import com.yoshino.args.exception.InsufficientArgumentsException;
import com.yoshino.args.exception.TooManyArgumentsException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * @author xiaoyi
 * 2022/12/10 19:01
 * @since
 **/
public class OptionParsers {

    public static OptionParser<Boolean> bool() {
        return (arguments, option) -> values(arguments, option, 0).map(it -> true).orElse(false) ;
    }

    public static <T> OptionParser<T> unary(T defaultValue, Function<String, T> valueParser) {
        return (arguments, option) -> values(arguments, option, 1).map(it -> parseValue(option, valueParser, it.get(0))).orElse(defaultValue);
    }

    public static <T> OptionParser<T[]> list(IntFunction<T[]> generator, Function<String, T> valueParser) {
        return (arguments, option) -> values(arguments, option)
            .map(it -> it.stream().map(value -> parseValue(option, valueParser, value)).toArray(generator))
            .orElseGet(() -> generator.apply(0));
    }

    private static <T> T parseValue(Option option, Function<String, T> valueParser, String value) {
        try {
            return valueParser.apply(value);
        } catch (Exception e) {
            throw new IllegalValueException(option.value(), value);
        }
    }

    static Optional<List<String>> values(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        return  Optional.ofNullable(index == -1 ? null : values(arguments, index));
    }

    static Optional<List<String>> values(List<String> arguments, Option option, int exceptedSize) {
        int index = arguments.indexOf("-" + option.value());
        if (index == -1) {
            return Optional.empty();
        } else {
            List<String> values = values(arguments, index);

            if (values.size() < exceptedSize) {
                throw new InsufficientArgumentsException(option.value());
            }
            if (values.size() > exceptedSize) {
                throw new TooManyArgumentsException(option.value());
            }

            return Optional.of(values);
        }
    }

    static List<String> values(List<String> arguments, int index) {
        int followingFlag = IntStream.range(index + 1, arguments.size())
            .filter(it -> arguments.get(it).matches("^-[a-zA-Z]+$"))
            .findFirst()
            .orElse(arguments.size());
        return arguments.subList(index + 1, followingFlag);
    }

}

package com.yoshino.args;

import com.yoshino.args.exception.UnsupportedTypeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author xiaoyi
 * 2022/12/8 00:15
 * @since
 **/
public class Args {

    public static <T> T parse(Class<T> optionsClass, String... args) {
        Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
        List<String> arguments = Arrays.asList(args);

        Object[] value = Arrays.stream(constructor.getParameters()).map(it -> parse(it, arguments)).toArray();

        try {
            return (T) constructor.newInstance(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static Object parse(Parameter parameter, List<String> arguments) {
        Option option = parameter.getAnnotation(Option.class);
        if (!PARSERS.containsKey(parameter.getType())) {
            throw new UnsupportedTypeException(parameter.getType());
        }
        return PARSERS.get(parameter.getType()).parseValue(arguments, option);
    }

    private static final Map<Class<?>, OptionParser> PARSERS = Map.of(
        boolean.class, OptionParsers.bool(),
        int.class, OptionParsers.unary(0, Integer::parseInt),
        String.class, OptionParsers.unary("", String::valueOf),
        String[].class, OptionParsers.list(String[]::new, String::valueOf),
        Integer[].class, OptionParsers.list(Integer[]::new, Integer::parseInt)
    );

}

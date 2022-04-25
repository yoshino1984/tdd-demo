package com.yoshino.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author xiaoyi
 * 2022/4/26 00:22
 * @since
 **/
public class Args {
    public static <T> T parse(Class<T> optionClass, String... args) {
        try {
            List<String> arguments = Arrays.asList(args);
            Constructor<?> constructor = optionClass.getDeclaredConstructors()[0];

            Object[] objects = Arrays.stream(constructor.getParameters()).map(it -> parse(arguments, it)).toList().toArray();

            return (T) constructor.newInstance(objects);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object parse(List<String> arguments, Parameter parameter) {
        return PARSERS.get(parameter.getType()).parse(arguments, parameter.getAnnotation(Option.class));
    }

    private static final Map<Class<?>, OptionParser> PARSERS = Map.of(
        boolean.class, new BoolOptionParser(),
        int.class, new IntOptionParser(),
        String.class, new StringOptionParser());

    static interface OptionParser {
        Object parse(List<String> arguments, Option option);
    }

    static class StringOptionParser implements OptionParser {
        @Override
        public Object parse(List<String> arguments, Option option) {
            int index = arguments.indexOf("-" + option.value());
            return String.valueOf(arguments.get(index + 1));
        }
    }

    static class IntOptionParser implements OptionParser {

        @Override
        public Object parse(List<String> arguments, Option option) {
            int index = arguments.indexOf("-" + option.value());
            return Integer.parseInt(arguments.get(index + 1));
        }
    }

    static class BoolOptionParser implements OptionParser {

        @Override
        public Object parse(List<String> arguments, Option option) {
            return arguments.contains("-" + option.value());
        }
    }

}

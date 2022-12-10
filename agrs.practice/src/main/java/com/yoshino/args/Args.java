package com.yoshino.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

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
        Object value = null;
        if (parameter.getType() == boolean.class) {
            value = new BoolOptionParser().parseValue(arguments, option);
        }
        if (parameter.getType() == int.class) {
            value = new IntOptionParser().parseValue(arguments, option);
        }
        if (parameter.getType() == String.class) {
            value = new StringOptionParser().parseValue(arguments, option);
        }
        return value;
    }

    interface OptionParser {
        Object parseValue(List<String> arguments, Option option);
    }


    public static class StringOptionParser implements OptionParser {

        @Override
        public Object parseValue(List<String> arguments, Option option) {
            int index = arguments.indexOf("-" + option.value());
            return arguments.get(index + 1);
        }
    }

    public static class IntOptionParser implements OptionParser {

        @Override
        public Object parseValue(List<String> arguments, Option option) {
            int index = arguments.indexOf("-" + option.value());
            return Integer.parseInt(arguments.get(index + 1));
        }
    }

    public static class BoolOptionParser implements OptionParser {

        @Override
        public Object parseValue(List<String> arguments, Option option) {
            return arguments.contains("-" + option.value());
        }
    }

}

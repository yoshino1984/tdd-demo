//package com.yoshino.args;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.Parameter;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author xiaoyi
// * 2022/4/19 00:59
// **/
//public class Args<T> {
//    private static final Map<Class<?>, OptionParser> PARSERS = Map.of(
//        boolean.class, OptionParsers.bool(),
//        int.class, OptionParsers.unary(0, Integer::parseInt),
//        String.class, OptionParsers.unary("", String::valueOf),
//        String[].class, OptionParsers.list(String[]::new, String::valueOf),
//        Integer[].class, OptionParsers.list(Integer[]::new, Integer::parseInt)
//    );
//
//    public static <T> T parse(Class<T> optionsClass, String... args) {
//        return new Args<T>(optionsClass, PARSERS).parse(args);
//    }
//
//    private Class<T> optionsClass;
//    Map<Class<?>, OptionParser> parsers;
//
//    public Args(Class<T> optionsClass, Map<Class<?>, OptionParser> parsers) {
//        this.optionsClass = optionsClass;
//        this.parsers = parsers;
//    }
//
//    public T parse(String... args) {
//        try {
//            List<String> arguments = Arrays.asList(args);
//            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
//
//            Object[] values = Arrays.stream(constructor.getParameters()).map(it -> parseOption(arguments, it)).toArray();
//
//            return (T) constructor.newInstance(values);
//        } catch (IllegalOptionException | UnsupportedOptionTypeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private Object parseOption(List<String> arguments, Parameter parameter) {
//        if (!parameter.isAnnotationPresent(Option.class)) {
//            throw new IllegalOptionException(parameter.getName());
//        }
//        Option option = parameter.getAnnotation(Option.class);
//        if (!parsers.containsKey(parameter.getType())) {
//            throw new UnsupportedOptionTypeException(option.value(), parameter.getType());
//        }
//        return parsers.get(parameter.getType()).parse(arguments, parameter.getAnnotation(Option.class));
//    }
//
//
//}

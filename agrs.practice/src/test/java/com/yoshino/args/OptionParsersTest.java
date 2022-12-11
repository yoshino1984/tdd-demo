package com.yoshino.args;

import com.yoshino.args.exception.IllegalValueException;
import com.yoshino.args.exception.InsufficientArgumentsException;
import com.yoshino.args.exception.TooManyArgumentsException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;

class OptionParsersTest {

    @Nested
    class UnaryOptionParser {

        @Test
        void should_not_accept_extra_argument_for_single_valued_option() {
            TooManyArgumentsException exception = assertThrows(TooManyArgumentsException.class, () -> {
                OptionParsers.unary(0, Integer::parseInt).parseValue(List.of("-p", "8080", "8081"), option("p"));
            });

            assertEquals("p", exception.getOption());
        }

        @ParameterizedTest
        @ValueSource(strings = {"-p", "-p -l"})
        void should_not_accept_insufficient_argument_for_single_valued_option(String arguments) {
            InsufficientArgumentsException exception = assertThrows(InsufficientArgumentsException.class, () -> {
                OptionParsers.unary(0, Integer::parseInt).parseValue(asList(arguments.split(" ")), option("p"));
            });

            assertEquals("p", exception.getOption());
        }

        @Test
        void should_set_default_value_to_0_for_int_option() {
            Integer value = OptionParsers.unary(0, Integer::parseInt).parseValue(List.of(), option("p"));
            assertEquals(0, value);
        }
    }

    @Nested
    class BoolOptionParser {

        @Test
        void should_not_accept_extra_argument_for_bool_option() {
            TooManyArgumentsException exception = assertThrows(TooManyArgumentsException.class,
                () -> OptionParsers.bool().parseValue(of("-l", "t"), option("l")));
            assertEquals("l", exception.getOption());
        }

        @Test
        void should_set_value_to_true_if_flag_present() {
            assertTrue(OptionParsers.bool().parseValue(of("-l"), option("l")));
        }

        @Test
        void should_set_value_to_false_if_option_not_present() {
            assertFalse(OptionParsers.bool().parseValue(of(), option("l")));
        }
    }

    @Nested
    class ListOptionParser {
        @Test
        void should_parse_list_string_value() {
            String[] value = OptionParsers.list(String[]::new, String::valueOf).parseValue(of("-g", "this", "is", "a", "list"), option("g"));
            assertArrayEquals(new String[] {"this", "is", "a", "list"}, value);
        }

        @Test
        void should_not_treat_negative_int_flag() {
            Integer[] value = OptionParsers.list(Integer[]::new, Integer::parseInt).parseValue(of("-d", "1", "2", "-3", "5"), option("d"));
            assertArrayEquals(new Integer[] {1, 2, -3, 5}, value);
        }

        @Test
        void should_set_empty_array_for_list_option() {
            String[] value = OptionParsers.list(String[]::new, String::valueOf).parseValue(of(), option("g"));
            assertEquals(0, value.length);
        }

        @Test
        void should_throw_exception_if_value_parser_cant_parse_value() {
            Function<String, String> parser = (it) -> {
                throw new RuntimeException();
            };

            IllegalValueException exception = assertThrows(IllegalValueException.class, () -> OptionParsers.list(String[]::new, parser).parseValue(of("-g", "this", "is", "a", "list"), option("g")));
            assertEquals("g", exception.getOption());
            assertEquals("this", exception.getValue());
        }


    }

    public static Option option(String value) {
        return new Option() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Option.class;
            }

            @Override
            public String value() {
                return value;
            }
        };
    }
}
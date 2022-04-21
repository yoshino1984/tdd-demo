package com.yoshino.args;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Function;

import static com.yoshino.args.BooleanOptionParserTest.option;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class SingleValuedOptionParserTest {

    @Test //sad path
    public void should_not_accept_extra_argument_for_int_option() {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
            new SingleValuedOptionParser<Integer>(0, Integer::parseInt).parse(asList("-p", "8080", "8081"), option("p"));
        });

        assertEquals("p", e.getOption());
    }

    @ParameterizedTest // sad path
    @ValueSource(strings = {"-p -l", "-p"})
    public void should_not_accept_insufficient_argument_for_single_valued_option(String arguments) {
        InsufficientArgumentsException e = assertThrows(InsufficientArgumentsException.class, () -> {
            new SingleValuedOptionParser<>(0, Integer::parseInt).parse(asList(arguments.split(" ")), option("p"));
        });

        assertEquals("p", e.getOption());
    }

    @Test // default value
    public void should_set_default_value_to_0_for_int_option() {
        Function<String, Object> whatever = (it) -> null;
        Object defaultValue = new Object();
        assertSame(defaultValue, new SingleValuedOptionParser<>(defaultValue, whatever).parse(asList(), option("p")));
    }


    @Test
    public void should_set_default_value_to_empty_for_int_option() {
        Object parsed = new Object();
        Function<String, Object> parser = (it) -> parsed;
        Object whatever = new Object();
        assertEquals(parsed, new SingleValuedOptionParser<>(whatever, parser).parse(asList("-p", "8080"), option("p")));
    }
}
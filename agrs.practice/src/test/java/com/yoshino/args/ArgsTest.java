package com.yoshino.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xiaoyi
 * 2022/12/8 00:12
 * @since
 **/
public class ArgsTest {


    @Test
    void should_set_value_to_true_if_flag_present() {
        BoolOption option = Args.parse(BoolOption.class, "-l");
        assertTrue(option.logging);
    }

    @Test
    void should_set_value_to_true_if_flag_not_present() {
        BoolOption option = Args.parse(BoolOption.class);
        assertFalse(option.logging);
    }

    record BoolOption(@Option("l") boolean logging) {
    }

    @Test
    void should_parse_int_value_for_int_option() {
        IntOption option = Args.parse(IntOption.class, "-p", "8080");
        assertEquals(8080, option.port());
    }

    record IntOption(@Option("p") int port) {
    }

    @Test
    void should_get_string_value_for_string_option() {
        StringOption option = Args.parse(StringOption.class, "-d", "/usr/logs");
        assertEquals("/usr/logs", option.directory());
    }
    record StringOption(@Option("d") String directory) {
    }

    @Test
    void should_parse_multi_options() {
        MultiOptions options = Args.parse(MultiOptions.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }

    record MultiOptions(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
    }


    // todo happy path
    // sad path
    // todo -l f too many arguments
    // todo -p 8080 8081
    // todo -p insufficient arguments
    // todo -p f error argument
    // default path
    // todo -l not present false
    // todo -p 0
    // todo -d ""







    @Test
    @Disabled
    void should_example_2() {
        ListOptions options = Args.parse(ListOptions.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");

        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.group());
        assertArrayEquals(new int[]{1, 2, -3, 5}, options.decimals());
    }



    record ListOptions(@Option("g") String[] group, @Option("d") int[] decimals) {
    }

}

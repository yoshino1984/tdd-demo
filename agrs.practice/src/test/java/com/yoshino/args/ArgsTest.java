package com.yoshino.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {
    // -l -p 8080 -d /usr/logs
    // todo -l
    @Test
    public void should_set_value_true_if_bool_flag_present() {
        assertTrue(Args.parse(BooleanOption.class, "-l").logging());
    }
    record BooleanOption(@Option("l") boolean logging) {}

    // todo -p 8080
    @Test
    public void should_set_port_if_int_flag_present() {
        assertEquals(8080, Args.parse(IntOption.class, "-p", "8080").port());
    }
    record IntOption(@Option("p") int port) {}

    // todo -d /usr/logs
    @Test
    public void should_set_directory_if_string_flag_present() {
        assertEquals("/usr/logs", Args.parse(StringOption.class, "-d", "/usr/logs").directory());
    }
    record StringOption(@Option("d") String directory) {}

    // -l -p 8080 -d /usr/logs
    @Test
    public void should_set_value_if_multi_option_present() {
        MultiOption value = Args.parse(MultiOption.class, "-l", "-p", "8080", "-d", "/usr/logs");

        assertTrue(value.logging());
        assertEquals(8080, value.port());
        assertEquals("/usr/logs", value.directory());
    }
    record MultiOption(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {}

    // -g this is a list -d 1 2 -3 5
    // todo -g this is a list
    // -d 1 2 -3 5

}

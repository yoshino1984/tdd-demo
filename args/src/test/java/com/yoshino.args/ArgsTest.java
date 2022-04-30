//package com.yoshino.args;
//
//import org.junit.jupiter.api.Test;
//
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//public class ArgsTest {
//
//    @Test
//    public void should_parse_multi_options() {
//        MultiOptions options = Args.parse(MultiOptions.class, "-l", "-p", "8080", "-d", "/usr/logs");
//        assertTrue(options.logging());
//        assertEquals(8080, options.port());
//        assertEquals("/usr/logs", options.directory());
//    }
//
//    record MultiOptions(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
//    }
//
//    @Test
//    public void should_throw_illegal_option_exception_if_Annotation_not_present() {
//        IllegalOptionException e = assertThrows(IllegalOptionException.class, () -> Args.parse(OptionsWithoutAnnotation.class, "-l", "-p", "8080", "-d", "/usr/logs"));
//
//        assertEquals("port", e.getParameter());
//    }
//
//    record OptionsWithoutAnnotation(@Option("l") boolean logging, int port, @Option("d") String directory) {
//    }
//
//    // sad path:
//    // - bool -l t / -l tf
//    // - int -p/ -p 8080 8081
//    // - string -d/ -d /usr/logs /usr/vars
//    // default value
//    // - bool : false
//    // - int : 0
//    // - string : ""
//
//
//    @Test
//    public void should_example_2() {
//        ListOptions options = Args.parse(ListOptions.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
//
//        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.group());
//        assertArrayEquals(new Integer[]{1, 2, -3, 5}, options.decimals());
//    }
//
//    record ListOptions(@Option("g") String[] group, @Option("d") Integer[] decimals) {
//    }
//
//
//    @Test
//    public void should_parse_options_if_option_parser_provided() {
//        OptionParser boolParser = mock(OptionParser.class);
//        OptionParser intParser = mock(OptionParser.class);
//        OptionParser stringParser = mock(OptionParser.class);
//
//        when(boolParser.parse(any(), any())).thenReturn(true);
//        when(intParser.parse(any(), any())).thenReturn(1000);
//        when(stringParser.parse(any(), any())).thenReturn("parsed");
//
//        Args<MultiOptions> args = new Args<>(MultiOptions.class, Map.of(boolean.class, boolParser, int.class, intParser, String.class, stringParser));
//
//        MultiOptions options = args.parse("-l", "-p", "8080", "-d", "/usr/logs");
//        assertTrue(options.logging());
//        assertEquals(1000, options.port());
//        assertEquals("parsed", options.directory());
//    }
//}

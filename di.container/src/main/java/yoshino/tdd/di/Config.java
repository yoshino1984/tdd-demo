package yoshino.tdd.di;

import java.lang.annotation.*;

/**
 * @author xiaoyi
 * 2023/1/13 00:14
 * @since
 **/
public interface Config {

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Export {
        Class<?> value();
    }
}

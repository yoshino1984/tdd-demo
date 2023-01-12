package yoshino.tdd.di;

import java.lang.annotation.Annotation;

/**
 * @author xiaoyi
 * 2023/1/13 00:13
 * @since
 **/
public record Component(Class<?> type, Annotation qualifier) {
}

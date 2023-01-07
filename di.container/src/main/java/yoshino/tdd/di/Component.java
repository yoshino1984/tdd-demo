package yoshino.tdd.di;

import java.lang.annotation.Annotation;

/**
 * @author xiaoyi
 * 2023/1/7 12:07
 * @since
 **/
record Component(Class<?> type, Annotation qualifier) {
}

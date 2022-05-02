package yoshino.tdd.di;

import java.util.List;

/**
 * @author xiaoyi
 * 2022/5/2 23:02
 * @since
 **/
interface ComponentProvider<T> {
    T get(Context context);

    List<Class<?>> getDependencies();
}

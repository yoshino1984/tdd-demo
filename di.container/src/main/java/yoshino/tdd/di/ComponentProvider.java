package yoshino.tdd.di;

import java.util.List;

/**
 * @author xiaoyi
 * 2022/12/31 13:22
 * @since
 **/
interface ComponentProvider<T> {
    T get(Context context);

    default List<Class<?>> getDependencies() {
        return List.of();
    }
}

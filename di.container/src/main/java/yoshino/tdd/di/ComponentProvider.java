package yoshino.tdd.di;

import java.lang.reflect.Type;
import java.util.List;

import static java.util.List.*;

/**
 * @author xiaoyi
 * 2022/12/31 13:22
 * @since
 **/
interface ComponentProvider<T> {
    T get(Context context);

    default List<Type> getDependencyTypes() {
        return of();
    }
}

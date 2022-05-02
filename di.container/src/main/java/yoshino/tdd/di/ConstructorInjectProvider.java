package yoshino.tdd.di;

import jakarta.inject.Inject;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static java.util.Arrays.stream;

/**
 * @author xiaoyi
 * 2022/5/2 23:03
 * @since
 **/
class ConstructorInjectProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Class<?>> dependencies;

    ConstructorInjectProvider(Constructor<T> injectConstructor, List<Class<?>> dependencies) {
        this.injectConstructor = injectConstructor;
        this.dependencies = dependencies;
    }

    static <Type> Constructor<Type> getInjectConstructor(Class<Type> implType) {
        List<Constructor<?>> collect = stream(implType.getDeclaredConstructors())
            .filter(it -> it.isAnnotationPresent(Inject.class)).toList();
        if (collect.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) collect.stream().findFirst().orElseGet(() -> {
            try {
                return implType.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    @Override
    public T get(Context context) {
        try {
            Object[] objects = stream(injectConstructor.getParameterTypes())
                .map(it -> context.get(it).get())
                .toList().toArray();
            return injectConstructor.newInstance(objects);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return dependencies;
    }
}

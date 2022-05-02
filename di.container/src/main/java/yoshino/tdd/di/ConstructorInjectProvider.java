package yoshino.tdd.di;

import jakarta.inject.Inject;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

/**
 * @author xiaoyi
 * 2022/5/2 23:03
 * @since
 **/
class ConstructorInjectProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Class<?>> dependencies;

    public ConstructorInjectProvider(Class<T> type) {
        this.injectConstructor = getInjectConstructor(type);
        this.injectFields = getInjectFields(type);
        this.dependencies = getMergedDependencies();
    }

    @Override
    public T get(Context context) {
        try {
            Object[] objects = stream(injectConstructor.getParameterTypes())
                .map(it -> context.get(it).get())
                .toList().toArray();
            T result = injectConstructor.newInstance(objects);
            for (Field field : injectFields) {
                field.setAccessible(true);
                field.set(result, context.get(field.getType()).get());
            }
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<Class<?>> getDependencies() {
        return dependencies;
    }

    private static <T> List<Field> getInjectFields(Class<T> type) {
        List<Field> result = new ArrayList<>();
        Class<?> currentType = type;
        while (currentType != Object.class) {
            result.addAll(stream(currentType.getDeclaredFields()).filter(it -> it.isAnnotationPresent(Inject.class)).toList());
            currentType = currentType.getSuperclass();
        }
        return result;
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implType) {
        List<Constructor<?>> collect = stream(implType.getDeclaredConstructors())
            .filter(it -> it.isAnnotationPresent(Inject.class)).toList();
        if (collect.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) collect.stream().findFirst().orElseGet(() -> {
            try {
                return implType.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    private List<Class<?>> getMergedDependencies() {
        return Stream.concat(stream(injectConstructor.getParameterTypes()), injectFields.stream().map(Field::getType)).toList();
    }
}

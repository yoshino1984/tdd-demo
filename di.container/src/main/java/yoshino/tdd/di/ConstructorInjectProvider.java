package yoshino.tdd.di;

import jakarta.inject.Inject;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

/**
 * @author xiaoyi
 * 2022/5/2 23:03
 * @since
 **/
class ConstructorInjectProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    public ConstructorInjectProvider(Class<T> type) {
        if (Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalComponentException();
        }
        this.injectConstructor = getInjectConstructor(type);
        this.injectFields = getInjectFields(type);
        this.injectMethods = getInjectMethods(type);
        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }


    @Override
    public T get(Context context) {
        try {
            T result = injectConstructor.newInstance(stream(injectConstructor.getParameterTypes()).map(it1 -> context.get(it1).get()).toArray());
            for (Field field : injectFields) {
                field.setAccessible(true);
                field.set(result, context.get(field.getType()).get());
            }
            for (Method method : injectMethods) {
                method.setAccessible(true);
                method.invoke(result, stream(method.getParameterTypes()).map(it -> context.get(it).get()).toArray());
            }
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<Class<?>> getDependencies() {
        return concat(concat(stream(injectConstructor.getParameterTypes()), injectFields.stream().map(Field::getType)),
            injectMethods.stream().flatMap(method -> stream(method.getParameterTypes()))).toList();
    }

    private static <T> List<Method> getInjectMethods(Class<T> type) {
        List<Method> result = new ArrayList<>();
        Class<?> currentType = type;
        while (currentType != Object.class) {
            result.addAll(stream(currentType.getDeclaredMethods())
                .filter(it -> it.isAnnotationPresent(Inject.class))
                .filter(it -> result.stream()
                    .noneMatch(method -> it.getName().equals(method.getName())
                        && Arrays.equals(it.getParameterTypes(), method.getParameterTypes())))
                .filter(it -> stream(type.getDeclaredMethods())
                    .filter(method -> !method.isAnnotationPresent(Inject.class))
                    .noneMatch(method -> it.getName().equals(method.getName())
                        && Arrays.equals(it.getParameterTypes(), method.getParameterTypes())))
                .toList());
            currentType = currentType.getSuperclass();
        }

        Collections.reverse(result);
        return result;
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

}

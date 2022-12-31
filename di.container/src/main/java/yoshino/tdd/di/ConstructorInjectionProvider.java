package yoshino.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

/**
 * @author xiaoyi
 * 2022/12/31 13:22
 * @since
 **/
class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
    private Constructor<?> injectConstructor;

    private List<Field> injectFields;

    public ConstructorInjectionProvider(Class<T> componentType) {
        this.injectConstructor = getInjectConstructor(componentType);
        this.injectFields = getInjectFields(componentType);
    }

    @Override
    public T get(Context context) {
        try {
            Object[] objects = stream(injectConstructor.getParameters()).map(it -> context.get(it.getType()).get()).toArray();
            T result = (T) injectConstructor.newInstance(objects);
            for (Field field : injectFields) {
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new IllegalComponentException();
                }
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
        return Stream.concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
            injectFields.stream().map(Field::getType)).collect(Collectors.toList());
    }

    private static <Type, Implementation extends Type> Constructor<?> getInjectConstructor(Class<Implementation> implementation) {
        List<Constructor<?>> injectedConstructors = stream(implementation.getConstructors()).filter(it -> it.isAnnotationPresent(Inject.class)).toList();
        if (injectedConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return injectedConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    private List<Field> getInjectFields(Class<T> componentType) {
        List<Field> result = new ArrayList<>();
        Class<?> current = componentType;
        while (current != Object.class) {
            result.addAll(stream(current.getDeclaredFields()).filter(it -> it.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return result;
    }
}

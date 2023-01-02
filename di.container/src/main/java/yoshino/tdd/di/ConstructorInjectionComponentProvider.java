package yoshino.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

/**
 * @author xiaoyi
 * 2022/12/31 13:22
 * @since
 **/
class ConstructorInjectionComponentProvider<T> implements ComponentProvider<T> {
    private Constructor<?> injectConstructor;

    private List<Field> injectFields;

    private List<Method> injectMethods;

    public ConstructorInjectionComponentProvider(Class<T> componentType) {
        if (Modifier.isAbstract(componentType.getModifiers())) {
            throw new IllegalComponentException();
        }

        this.injectConstructor = getInjectConstructor(componentType);
        this.injectFields = getInjectFields(componentType);
        this.injectMethods = getInjectMethods(componentType);

        if (injectFields.stream().anyMatch(it -> Modifier.isFinal(it.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(it -> it.getTypeParameters().length > 0)) {
            throw new IllegalComponentException();
        }
    }

    @Override
    public T get(Context context) {
        try {
            Object[] objects = stream(injectConstructor.getParameters()).map(it -> context.get(it.getType()).get()).toArray();
            T result = (T) injectConstructor.newInstance(objects);
            for (Field field : injectFields) {
                field.setAccessible(true);
                field.set(result, context.get(field.getType()).get());
            }
            for (Method method : injectMethods) {
                method.invoke(result, stream(method.getParameterTypes()).map(t -> context.get(t).get()).toArray());
            }
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return concat(concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
            injectFields.stream().map(Field::getType)),
            injectMethods.stream().flatMap(m -> stream(m.getParameterTypes())))
            .collect(Collectors.toList());
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

    private List<Method> getInjectMethods(Class<T> componentType) {
        List<Method> result = new ArrayList<>();
        Class<?> current = componentType;
        while (current != Object.class) {
            result.addAll(stream(current.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Inject.class))
                    .filter(m -> result.stream()
                        .noneMatch(it -> it.getName().equals(m.getName()) && Arrays.equals(it.getParameterTypes(), m.getParameterTypes())))
                    .filter(m -> stream(componentType.getDeclaredMethods()).filter(it -> !it.isAnnotationPresent(Inject.class))
                        .noneMatch(it -> it.getName().equals(m.getName()) && Arrays.equals(it.getParameterTypes(), m.getParameterTypes())))
                .toList());
            current = current.getSuperclass();
        }
        Collections.reverse(result);
        return result;
    }
}

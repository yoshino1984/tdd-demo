package yoshino.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

/**
 * @author xiaoyi
 * 2022/12/31 13:22
 * @since
 **/
class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
    private Constructor<?> constructor;

    public ConstructorInjectionProvider(Class<T> componentType) {
        this.constructor = getInjectConstructor(componentType);
    }

    @Override
    public T get(Context context) {

        try {
            Object[] objects = stream(constructor.getParameters()).map(it -> context.get(it.getType()).get()).toArray();
            return (T) constructor.newInstance(objects);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return stream(constructor.getParameters()).map(Parameter::getType).collect(Collectors.toList());
    }

    private static <Type, Implementation extends Type> Constructor<?> getInjectConstructor(Class<Implementation> implementation) {
        List<Constructor<?>> injectedConstructors = stream(implementation.getConstructors()).filter(it -> it.isAnnotationPresent(Inject.class)).toList();
        if (injectedConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return injectedConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }
}

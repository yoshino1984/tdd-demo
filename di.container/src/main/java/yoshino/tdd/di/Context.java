package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.stream;

/**
 * @author xiaoyi
 * 2022/5/1 17:49
 * @since
 **/
public class Context {
    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, () -> instance);
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(providers.get(type)).map(it -> (Type) providers.get(type).get());
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementatio) {
        Constructor<Implementation> constructor = getInjectConstructor(implementatio);

        providers.put(type, () -> {
            try {
                Object[] objects = stream(constructor.getParameterTypes())
                    .map(it -> get(it).orElseThrow(DependencyNotFoundException::new))
                    .toList().toArray();
                return constructor.newInstance(objects);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <Type> Constructor<Type> getInjectConstructor(Class<Type> implType) {
        List<Constructor<?>> collect = stream(implType.getDeclaredConstructors())
            .filter(it -> it.isAnnotationPresent(Inject.class)).toList();
        if (collect.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) collect.stream().findFirst().orElseGet(() -> getDefaultConstructor(implType));
    }

    private <Type> Constructor<Type> getDefaultConstructor(Class<Type> implType) {
        try {
            return implType.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }
}

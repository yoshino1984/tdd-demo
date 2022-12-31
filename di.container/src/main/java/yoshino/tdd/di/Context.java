package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.*;

/**
 * @author xiaoyi
 * 2022/12/25 16:21
 * @since
 **/
public class Context {

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, () -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?> injectedConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider<>(injectedConstructor, implementation));
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(providers.get(type)).map(it -> (Type) it.get());
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


    class ConstructorInjectionProvider<T> implements Provider<T> {
        private Constructor<?> constructor;
        private boolean constructed;
        private Class<?> componentType;

        public ConstructorInjectionProvider(Constructor<?> constructor, Class<T> componentType) {
            this.constructor = constructor;
            this.componentType = componentType;
        }

        @Override
        public T get() {
            if (constructed) {
                throw new CyclicDependenciesException(componentType);
            }
            constructed = true;

            try {
                Object[] objects = stream(constructor.getParameters())
                    .map(it -> Context.this.get(it.getType()).orElseThrow(() -> new DependencyNotFoundException(it.getType())))
                    .toArray();
                return (T) constructor.newInstance(objects);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (CyclicDependenciesException e)
            {
                throw new CyclicDependenciesException(e, componentType);
            }
        }
    }

}

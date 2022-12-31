package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.*;

/**
 * @author xiaoyi
 * 2022/12/25 16:21
 * @since
 **/
public class ContextConfig {
    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    private Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
        dependencies.put(type, List.of());
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?> injectedConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider<>(injectedConstructor, implementation));
        dependencies.put(type, stream(injectedConstructor.getParameters()).map(Parameter::getType).collect(Collectors.toList()));
    }


    public Context getContext() {
        for (Class<?> component : dependencies.keySet()) {
            for (Class<?> dependency : dependencies.get(component)) {
                if (!dependencies.containsKey(dependency)) {
                    throw new DependencyNotFoundException(dependency);
                }
            }
        }
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(it -> (Type) it.get(this));
            }
        };
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

    interface ComponentProvider<T> {
        T get(Context context);
    }

    class ConstructorInjectionProvider<T> implements ComponentProvider<T>{
        private Constructor<?> constructor;
        private boolean constructing;
        private Class<?> componentType;

        public ConstructorInjectionProvider(Constructor<?> constructor, Class<T> componentType) {
            this.constructor = constructor;
            this.componentType = componentType;
        }

        @Override
        public T get(Context context) {
            if (constructing) {
                throw new CyclicDependenciesException(componentType);
            }

            try {
                constructing = true;
                Object[] objects = stream(constructor.getParameters()).map(it -> context.get(it.getType()).get()).toArray();
                return (T) constructor.newInstance(objects);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (CyclicDependenciesException e) {
                throw new CyclicDependenciesException(e, componentType);
            } finally {
                constructing = false;
            }
        }
    }

}

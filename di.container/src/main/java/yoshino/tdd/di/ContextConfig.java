package yoshino.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author xiaoyi
 * 2022/12/25 16:21
 * @since
 **/
public class ContextConfig {
    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }


    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {

            @Override
            public Optional getType(Type type) {
                if (isContainerType(type)) {
                    return get((ParameterizedType) type);
                }
                return get((Class<?>) type);
            }

            private <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(it -> (Type) it.get(this));
            }

            private Optional get(ParameterizedType type) {
                if (type.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                return Optional.ofNullable(providers.get(getComponentType(type))).map(provider -> (Provider<Object>) () -> provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencyTypes()) {
            if (dependency instanceof Class) {
                checkComponentDependency((Class<?>) dependency, visiting);
            }
            if (isContainerType(dependency)) {
                checkContainerDependency(dependency);
            }
        }
    }

    private void checkContainerDependency(Type dependency) {
        Class<?> type = getComponentType(dependency);
        if (!providers.containsKey(type)) {
            throw new DependencyNotFoundException(type);
        }
    }

    private static Class<?> getComponentType(Type dependency) {
        return (Class<?>) ((ParameterizedType)dependency).getActualTypeArguments()[0];
    }

    private static boolean isContainerType(Type dependency) {
        return dependency instanceof ParameterizedType;
    }

    private void checkComponentDependency(Class<?> dependency, Stack<Class<?>> visiting) {
        if (!providers.containsKey(dependency)) {
            throw new DependencyNotFoundException(dependency);
        }
        if (visiting.contains(dependency)) {
            throw new CyclicDependenciesException(visiting);
        }
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

}

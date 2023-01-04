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
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(it -> (Type) it.get(this));
            }

            @Override
            public Optional get(ParameterizedType type) {
                if (type.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                Type componentType = type.getActualTypeArguments()[0];
                return Optional.ofNullable(providers.get(componentType))
                    .map(provider -> (Provider<Object>) () -> provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencyTypes()) {
            if (dependency instanceof Class) {
                checkDependency((Class<?>) dependency, visiting);
            }
            if (dependency instanceof ParameterizedType) {
                Class<?> type = (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
                if (!providers.containsKey(type)) {
                    throw new DependencyNotFoundException(type);
                }
            }
        }
    }

    private void checkDependency(Class<?> dependency, Stack<Class<?>> visiting) {
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

package yoshino.tdd.di;

import jakarta.inject.Provider;

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
            public Optional<?> get(Ref ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(providers.get(ref.getComponent())).map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent())).map(it -> (Object) it.get(this));
            }

        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Context.Ref dependency : providers.get(component).getDependencyRefs()) {
            if (!providers.containsKey(dependency.getComponent())) {
                throw new DependencyNotFoundException(dependency.getComponent());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponent())) {
                    throw new CyclicDependenciesException(visiting);
                }
                visiting.push(dependency.getComponent());
                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }
        }
    }
}

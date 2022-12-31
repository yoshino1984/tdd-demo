package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public <Type> Type get(Class<Type> type) {
        if (providers.containsKey(type)) {
            return (Type) providers.get(type).get();
        }
        throw new DependencyNotFoundException(type);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?> injectedConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider(injectedConstructor));
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

        public ConstructorInjectionProvider(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public T get() {
            if (constructed) {
                throw new CyclicDependenciesException();
            }
            constructed = true;

            try {
                Object[] objects = stream(constructor.getParameters()).map(parameter -> Context.this.get(parameter.getType())).toArray();
                return (T) constructor.newInstance(objects);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}

package yoshino.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Arrays.*;

/**
 * @author xiaoyi
 * 2022/12/25 16:21
 * @since
 **/
public class Context {

    private Map<Class<?>, Supplier<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, () -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, () -> {
            try {
                Constructor<?> injectedConstructor = stream(implementation.getConstructors()).filter(it -> it.isAnnotationPresent(Inject.class)).findFirst()
                    .orElseGet(() -> {
                        try {
                            return implementation.getConstructor();
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    });

                Object[] objects = stream(injectedConstructor.getParameters()).map(parameter -> get(parameter.getType())).toArray();

                return injectedConstructor.newInstance(objects);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }


}

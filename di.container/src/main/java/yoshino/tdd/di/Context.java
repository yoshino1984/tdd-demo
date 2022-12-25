package yoshino.tdd.di;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author xiaoyi
 * 2022/12/25 16:21
 * @since
 **/
public class Context {

    private Map<Class<?>, Supplier<?>> providers = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        providers.put(type, () -> instance);
    }

    public <ComponentType, ComponentTypeImpl extends ComponentType>
    void bind(Class<ComponentType> type, Class<ComponentTypeImpl> implementation) {
        providers.put(type, () -> {
            try {
                return implementation.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        return (ComponentType) providers.get(type).get();
    }


}

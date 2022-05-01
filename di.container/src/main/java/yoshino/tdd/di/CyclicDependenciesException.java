package yoshino.tdd.di;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author xiaoyi
 * 2022/5/1 22:39
 * @since
 **/
public class CyclicDependenciesException extends RuntimeException {
    private final List<Class<?>> components;

    public CyclicDependenciesException(Class<?> component) {
        this.components = List.of(component);
    }

    public <T> CyclicDependenciesException(Class<T> componentType, CyclicDependenciesException e) {
        this.components = new ArrayList<>();
        this.components.add(componentType);
        this.components.addAll(e.getComponents());
    }

    public List<Class<?>> getComponents() {
        return components;
    }
}

package yoshino.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author xiaoyi
 * 2023/1/7 16:09
 * @since
 **/
public
class ComponentRef<ComponentType> {

    public static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> type, Annotation qualifier) {
        return new ComponentRef<>(type, qualifier);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Type type, Annotation qualifier) {
        return new ComponentRef<>(type, qualifier);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> type) {
        return new ComponentRef<>(type);
    }

    private Type container;

    private Component component;

    ComponentRef(Type container, Annotation qualifier) {
        init(container, qualifier);
    }

    ComponentRef(Class<ComponentType> type) {
        init(type, null);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, null);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.component = new Component((Class<ComponentType>) container.getActualTypeArguments()[0], qualifier);
        } else {
            this.component = new Component((Class<ComponentType>) type, qualifier);
        }
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponentType() {
        return component.type();
    }

    public Component component() {
        return component;
    }

    public boolean isContainer() {
        return this.container != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef<?> componentRef = (ComponentRef<?>) o;
        return Objects.equals(container, componentRef.container) && component.equals(componentRef.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}

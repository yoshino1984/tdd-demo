package yoshino.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/**
 * @author xiaoyi
 * 2022/12/31 12:26
 * @since
 **/
public interface Context {

    <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref);

    class Ref<ComponentType> {

        public static Ref of(Type type) {
            return new Ref(type, null);
        }
        public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> type, Annotation qualifier) {
            return new Ref<>(type, qualifier);
        }

        public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> type) {
            return new Ref<>(type);
        }

        private Type container;

        private Component component;

        Ref(Type container, Annotation qualifier) {
            init(container, qualifier);
        }

        Ref(Class<ComponentType> type) {
            init(type, null);
        }

        protected Ref() {
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
            Ref<?> ref = (Ref<?>) o;
            return Objects.equals(container, ref.container) && component.equals(ref.component);
        }

        @Override
        public int hashCode() {
            return Objects.hash(container, component);
        }
    }
}

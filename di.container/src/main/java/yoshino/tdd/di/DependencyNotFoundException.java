package yoshino.tdd.di;

/**
 * @author xiaoyi
 * 2022/5/1 22:08
 * @since
 **/
public class DependencyNotFoundException extends RuntimeException {
    private final Class<?> component;
    private final Class<?> dependency;

    public DependencyNotFoundException(Class<?> component, Class<?> dependency) {
        this.component = component;
        this.dependency = dependency;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }
}

package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Nested;


public class ContainerTest {

    @Nested
    public class DependenciesSelectionConstruction {

        @Nested
        class ProviderType {

        }

    }

    @Nested
    public class LifecycleConstruction {

    }
}


interface TestComponent {

}

interface Dependency {
}

interface AnotherDependency {
}

class ComponentWithDefaultConstructor implements TestComponent {

    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithDependencyInjectedConstructor implements TestComponent {
    private Dependency dependency;

    @Inject
    public ComponentWithDependencyInjectedConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectedConstructors implements TestComponent {

    @Inject
    public ComponentWithMultiInjectedConstructors(String name) {
    }

    @Inject
    public ComponentWithMultiInjectedConstructors(String name, Double value) {
    }
}

class ComponentWithNoInjectedNorDefaultConstructor implements TestComponent {

    public ComponentWithNoInjectedNorDefaultConstructor(String name) {
    }
}

class DependencyWithDependencyInjected implements Dependency {
    private String name;

    @Inject
    public DependencyWithDependencyInjected(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

class DependencyDependedOnComponent implements Dependency {
    private TestComponent component;

    @Inject
    public DependencyDependedOnComponent(TestComponent component) {
        this.component = component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    private AnotherDependency dependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency dependency) {
        this.dependency = dependency;
    }
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    private TestComponent component;

    @Inject
    public AnotherDependencyDependedOnComponent(TestComponent component) {
        this.component = component;
    }
}


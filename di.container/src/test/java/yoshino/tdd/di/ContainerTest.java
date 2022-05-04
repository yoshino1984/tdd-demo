package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import yoshino.tdd.di.exception.CyclicDependenciesException;
import yoshino.tdd.di.exception.DependencyNotFoundException;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xiaoyi
 **/
public class ContainerTest {
    private ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            assertEquals(instance, config.getContext().get(Component.class).get());
        }

        @Test
        public void should_return_null_if_component_not_found() {
            assertTrue(config.getContext().get(Component.class).isEmpty());
        }

        @Nested
        public class DependencyCheck {

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext().get(Component.class).get();
                });

                assertEquals(Dependency.class, e.getDependency());
                assertEquals(Component.class, e.getComponent());
            }

            @Test
            public void should_throw_exception_if_present_cyclic_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependOnComponentConstructor.class);
                CyclicDependenciesException e = assertThrows(CyclicDependenciesException.class, () -> {
                    config.getContext();
                });

                List<Class<?>> components = e.getComponents();

                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_present_transitive_cyclic_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependOnAnotherDependencyConstructor.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependOnComponentConstructor.class);
                CyclicDependenciesException e = assertThrows(CyclicDependenciesException.class, () -> {
                    config.getContext();
                });

                List<Class<?>> components = e.getComponents();

                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }
        }

    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }
}


interface Component {
}

interface Dependency {
}

interface AnotherDependency {
}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements Component {
    private final Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component {

    @Inject
    public ComponentWithMultiInjectConstructors(String name, Dependency dependency) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class ComponentWithNoInjectNorDefaultConstructor implements Component {

    public ComponentWithNoInjectNorDefaultConstructor(String name) {
    }
}


class DependencyWithInjectConstructor implements Dependency {
    private String dependency;

    @Inject
    DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependOnComponentConstructor implements Dependency {
    private Component component;

    @Inject
    DependencyDependOnComponentConstructor(Component component) {
        this.component = component;
    }
}


class AnotherDependencyDependOnComponentConstructor implements AnotherDependency {
    private Component component;

    @Inject
    AnotherDependencyDependOnComponentConstructor(Component component) {
        this.component = component;
    }
}

class DependencyDependOnAnotherDependencyConstructor implements Dependency {
    private AnotherDependency dependency;

    @Inject
    DependencyDependOnAnotherDependencyConstructor(AnotherDependency dependency) {
        this.dependency = dependency;
    }
}
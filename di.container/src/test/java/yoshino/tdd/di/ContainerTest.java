package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


public class ContainerTest {

    @Nested
    public class ComponentConstruction {

        ContextConfig config;

        @BeforeEach
        public void setUp() {
            config = new ContextConfig();
        }

        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };

            config.bind(Component.class, instance);

            assertEquals(instance, config.getContext().get(Component.class).get());
        }

        // todo abstract class
        // todo interface

        @Test
        public void should_return_empty_if_cant_found_component() {
            Optional<Component> component = config.getContext().get(Component.class);

            assertTrue(component.isEmpty());
        }

        @Nested
        public class ConstructorInjection {

            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = config.getContext().get(Component.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            @Test
            public void should_bind_type_to_a_class_with_dependency_injected() {
                Dependency dependency = new Dependency() {
                };

                config.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);
                config.bind(Dependency.class, dependency);

                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertEquals(dependency, ((ComponentWithDependencyInjectedConstructor) instance).getDependency());
            }

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies_injected() {
                config.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);
                config.bind(Dependency.class, DependencyWithDependencyInjected.class);
                config.bind(String.class, "injected dependencies");

                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithDependencyInjectedConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("injected dependencies", ((DependencyWithDependencyInjected) dependency).getName());
            }

            @Test
            public void should_throw_exception_when_class_with_multi_injected_constructor() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectedConstructors.class);
                });
            }

            @Test
            public void should_throw_exception_when_class_no_injected_constructor_nor_default_constructor() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectedNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_throw_exception_if_cant_find_dependency() {
                config.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertSame(exception.getInstance(), Dependency.class);
            }

            @Test
            public void should_throw_exception_if_exist_cyclic_dependencies() {
                config.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class, () -> config.getContext());

                assertTrue(exception.getDependencies().contains(Component.class));
                assertTrue(exception.getDependencies().contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_exist_transitive_cyclic_dependencies() {
                config.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class, () -> config.getContext());

                assertTrue(exception.getDependencies().contains(Component.class));
                assertTrue(exception.getDependencies().contains(Dependency.class));
                assertTrue(exception.getDependencies().contains(AnotherDependency.class));
            }
        }

    }

    @Nested
    public class DependenciesSelectionConstruction {

    }

    @Nested
    public class LifecycleConstruction {

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

class ComponentWithDependencyInjectedConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithDependencyInjectedConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectedConstructors implements Component {

    @Inject
    public ComponentWithMultiInjectedConstructors(String name) {
    }

    @Inject
    public ComponentWithMultiInjectedConstructors(String name, Double value) {
    }
}

class ComponentWithNoInjectedNorDefaultConstructor implements Component {

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
    private Component component;

    @Inject
    public DependencyDependedOnComponent(Component component) {
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
    private Component component;

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}


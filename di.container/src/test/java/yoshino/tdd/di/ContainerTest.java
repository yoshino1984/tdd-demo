package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

        @Test
        public void should_return_empty_if_component_not_defined() {
            Optional<Component> component = config.getContext().get(Component.class);

            assertTrue(component.isEmpty());
        }

        @Nested
        class DependencyCheck {

            @Test
            public void should_throw_exception_if_cant_find_dependency() {
                config.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertSame(exception.getInstance(), Dependency.class);
            }

            @ParameterizedTest
            @MethodSource
            public void should_throw_exception_if_exist_cyclic_dependencies(Class<? extends Component> component,
                                                                            Class<? extends Dependency> dependency) {
                config.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class, () -> config.getContext());

                assertTrue(exception.getDependencies().contains(Component.class));
                assertTrue(exception.getDependencies().contains(Dependency.class));
            }

            private static Stream<Arguments> should_throw_exception_if_exist_cyclic_dependencies() {
                List<Arguments> result = new ArrayList<>();
                for (Named<? extends Class<? extends Component>> component : List.of(
                    Named.of("inject constructor", ComponentInjectConstructor.class),
                    Named.of("inject field", ComponentInjectField.class),
                    Named.of("inject method", ComponentInjectMethod.class))) {
                    for (Named<? extends Class<? extends Dependency>> dependency : List.of(
                        Named.of("inject constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("inject field", CyclicDependencyInjectField.class),
                        Named.of("inject method", CyclicDependencyInjectMethod.class))) {
                        result.add(Arguments.of(component, dependency));
                    }
                }
                return result.stream();
            }

            class ComponentInjectConstructor implements Component {
                private Dependency dependency;
                @Inject
                public ComponentInjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            class ComponentInjectField implements Component {
                @Inject
                private Dependency dependency;

                public ComponentInjectField() {
                }
            }

            class ComponentInjectMethod implements Component {
                private Dependency dependency;

                public ComponentInjectMethod() {
                }
                @Inject
                public void install(Dependency dependency) {}
            }

            class CyclicDependencyInjectConstructor implements Dependency {
                private Component component;
                @Inject
                public CyclicDependencyInjectConstructor(Component component) {
                }
            }

            class CyclicDependencyInjectField implements Dependency {
                @Inject
                private Component component;

                public CyclicDependencyInjectField() {
                }
            }

            class CyclicDependencyInjectMethod implements Dependency {

                public CyclicDependencyInjectMethod() {
                }
                @Inject
                public void install(Component component) {}
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


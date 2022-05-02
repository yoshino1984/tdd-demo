package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import yoshino.tdd.di.exception.CyclicDependenciesException;
import yoshino.tdd.di.exception.DependencyNotFoundException;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xiaoyi
 **/
public class ContainerTest {
    private ContextConfig contextConfig;

    @BeforeEach
    public void setUp() {
        contextConfig = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            contextConfig.bind(Component.class, instance);

            assertEquals(instance, contextConfig.getContext().get(Component.class).get());
        }

        @Test
        public void should_return_null_if_component_not_found() {
            assertTrue(contextConfig.getContext().get(Component.class).isEmpty());
        }

        // todo abstract
        // todo interface

        @Nested
        public class ConstructionInjection {
            @Test
            public void should_bind_type_to_class_with_default_no_args_constructor() {
                contextConfig.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = contextConfig.getContext().get(Component.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }

            @Test
            public void should_bind_type_to_class_with_a_inject_constructor() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                Dependency dependency = new Dependency() {
                };
                contextConfig.bind(Dependency.class, dependency);

                Component instance = contextConfig.getContext().get(Component.class).get();

                assertNotNull(instance);
                assertEquals(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            @Test
            public void should_bind_type_to_class_with_transitive_inject_constructor() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
                contextConfig.bind(String.class, "dependency");

                Component instance = contextConfig.getContext().get(Component.class).get();

                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertEquals("dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors() {
                assertThrows(IllegalComponentException.class, () -> {
                    contextConfig.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor() {
                assertThrows(IllegalComponentException.class, () -> {
                    contextConfig.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> {
                    contextConfig.getContext().get(Component.class).get();
                });

                assertEquals(Dependency.class, e.getDependency());
                assertEquals(Component.class, e.getComponent());
            }

            @Test
            public void should_throw_exception_if_present_cyclic_dependencies() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyDependOnComponentConstructor.class);
                CyclicDependenciesException e = assertThrows(CyclicDependenciesException.class, () -> {
                    contextConfig.getContext();
                });

                List<Class<?>> components = e.getComponents();

                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_present_transitive_cyclic_dependencies() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyDependOnAnotherDependencyConstructor.class);
                contextConfig.bind(AnotherDependency.class, AnotherDependencyDependOnComponentConstructor.class);
                CyclicDependenciesException e = assertThrows(CyclicDependenciesException.class, () -> {
                    contextConfig.getContext();
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


interface Dependency {
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

interface AnotherDependency {}
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
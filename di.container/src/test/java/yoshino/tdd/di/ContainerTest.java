package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xiaoyi
 **/
public class ContainerTest {
    private Context context;

    @BeforeEach
    public void setUp() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            context.bind(Component.class, instance);

            assertEquals(instance, context.get(Component.class).get());
        }
        // todo abstract
        // todo interface
        // todo component does not exist
        @Test
        public void should_return_null_if_component_not_found() {
            assertTrue(context.get(Component.class).isEmpty());
        }

        @Nested
        public class ConstructionInjection {
            @Test
            public void should_bind_type_to_class_with_default_no_args_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }

            @Test
            public void should_bind_type_to_class_with_a_inject_constructor() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                Dependency dependency = new Dependency() {
                };
                context.bind(Dependency.class, dependency);

                Component instance = context.get(Component.class).get();

                assertNotNull(instance);
                assertEquals(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            @Test
            public void should_bind_type_to_class_with_transitive_inject_constructor() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "dependency");

                Component instance = context.get(Component.class).get();

                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertEquals("dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            // todo multi inject constructor
            @Test
            public void should_throw_exception_if_multi_inject_constructors() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            // todo no inject constructor nor default constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class);
                });
            }

            // todo dependency not found
            @Test
            public void should_throw_exception_if_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                assertThrows(DependencyNotFoundException.class, () -> {
                    context.get(Component.class).get();
                });
            }

            // todo cycle dependency
            @Test
            public void should_throw_exception_if_present_cyclic_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependOnComponentConstructor.class);
                assertThrows(CyclicDependenciesException.class, () -> {
                    context.get(Component.class);
                });
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
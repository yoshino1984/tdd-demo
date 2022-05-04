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


        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent(Dependency dependency) {
            }
        }
        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(AbstractComponent.class));
        }
        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(Component.class));
        }

        @Nested
        public class ConstructionInjection {
            @Test
            public void should_bind_type_to_class_with_default_no_args_constructor() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = config.getContext().get(Component.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }

            @Test
            public void should_bind_type_to_class_with_a_inject_constructor() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                Component instance = config.getContext().get(Component.class).get();

                assertNotNull(instance);
                assertEquals(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            @Test
            public void should_bind_type_to_class_with_transitive_inject_constructor() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                config.bind(String.class, "dependency");

                Component instance = config.getContext().get(Component.class).get();

                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertEquals("dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class);
                });
            }

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

        @Nested
        public class FieldInjection {
            static class ComponentWithFieldInjection implements Component {
                @Inject
                Dependency dependency;
            }

            static class ComponentSubclassWithFieldInjection extends ComponentWithFieldInjection {

            }

            @Test
            public void should_inject_dependency_via_field() {
                config.bind(Component.class, ComponentWithFieldInjection.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                Optional<Component> component = config.getContext().get(Component.class);

                assertSame(dependency, ((ComponentWithFieldInjection) component.get()).dependency);
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                config.bind(Component.class, ComponentSubclassWithFieldInjection.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                Optional<Component> component = config.getContext().get(Component.class);

                assertSame(dependency, ((ComponentSubclassWithFieldInjection) component.get()).dependency);
            }

            @Test
            public void should_include_field_dependency_in_dependencies() {
                ConstructorInjectProvider<ComponentWithFieldInjection> provider = new ConstructorInjectProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
            }

            static class ComponentWithFinalFieldInjection implements Component {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(ComponentWithFinalFieldInjection.class));
            }


        }

        @Nested
        public class MethodInjection {
            static class MethodInjectionWithNoDependency implements Component {
                boolean called = false;

                @Inject
                public void install() {
                    called = true;
                }
            }

            @Test
            public void should_call_inject_no_dependency_method_via_method() {
                config.bind(Component.class, MethodInjectionWithNoDependency.class);

                Component component = config.getContext().get(Component.class).get();

                assertTrue(((MethodInjectionWithNoDependency) component).called);
            }

            static class MethodInjectionInjectDependency implements Component {
                Dependency dependency;

                @Inject
                public void setDependency(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_method() {
                config.bind(Component.class, MethodInjectionInjectDependency.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                Component component = config.getContext().get(Component.class).get();

                assertSame(dependency, ((MethodInjectionInjectDependency) component).dependency);
            }

            @Test
            public void should_dependency_in_dependencies_via_inject_method() {
                ConstructorInjectProvider<MethodInjectionInjectDependency> provider = new ConstructorInjectProvider<>(MethodInjectionInjectDependency.class);
                assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
            }

            static class MethodInjectionSuperclass implements Component {
                int called = 0;

                @Inject
                public void install() {
                    called++;
                }

            }

            static class MethodInjectionSubclass extends MethodInjectionSuperclass {
                int anotherCalled = 0;

                @Inject
                public void anotherInstall() {
                    anotherCalled = called + 1;
                }
            }

            @Test
            public void should_call_superclass_inject_method_first_by_inject_subclass() {
                config.bind(Component.class, MethodInjectionSubclass.class);

                MethodInjectionSubclass component = (MethodInjectionSubclass) config.getContext().get(Component.class).get();

                assertEquals(1, component.called);
                assertEquals(2, component.anotherCalled);
            }


            static class MethodInjectionSubclassOverrideMethod extends MethodInjectionSuperclass {

                @Inject
                @Override
                public void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_subclass_override_method_via_inject_method() {
                config.bind(Component.class, MethodInjectionSubclassOverrideMethod.class);


                MethodInjectionSubclassOverrideMethod component = (MethodInjectionSubclassOverrideMethod) config.getContext().get(Component.class).get();

                assertEquals(1, component.called);
            }


            static class MethodInjectionSubclassOverrideNoInjectMethod extends MethodInjectionSuperclass {

                @Override
                public void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_invoke_no_inject_method_via_method() {
                config.bind(Component.class, MethodInjectionSubclassOverrideNoInjectMethod.class);


                MethodInjectionSubclassOverrideNoInjectMethod component = (MethodInjectionSubclassOverrideNoInjectMethod) config.getContext().get(Component.class).get();

                assertEquals(0, component.called);
            }

            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {

                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(InjectMethodWithTypeParameter.class));
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
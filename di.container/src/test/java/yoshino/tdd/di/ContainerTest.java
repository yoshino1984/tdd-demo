package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

        @Nested
        class FieldInjection {
            static class ComponentWithFieldInject {
                @Inject
                Dependency dependency;
            }

            static class ComponentWithFieldInjectSubclass extends ComponentWithFieldInject {}

            @Test
            public void should_inject_via_field() {
                Dependency dependency = new Dependency() {
                };

                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInject.class, ComponentWithFieldInject.class);

                ComponentWithFieldInject component = config.getContext().get(ComponentWithFieldInject.class).get();

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_field_via_superclass() {
                Dependency dependency = new Dependency() {
                };

                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjectSubclass.class, ComponentWithFieldInjectSubclass.class);

                ComponentWithFieldInjectSubclass component = config.getContext().get(ComponentWithFieldInjectSubclass.class).get();

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_return_correct_dependencies_via_field_inject() {
                ConstructorInjectionProvider<ComponentWithFieldInjectSubclass> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjectSubclass.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
            }

            static class ComponentWithFinalFieldInject {
                @Inject
                final Dependency dependency;

                public ComponentWithFinalFieldInject() {
                    this.dependency = null;
                }
            }

            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                Dependency dependency = new Dependency() {
                };

                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFinalFieldInject.class, ComponentWithFinalFieldInject.class);

                assertThrows(IllegalComponentException.class, () -> config.getContext().get(ComponentWithFinalFieldInject.class).get());
            }
        }

        @Nested
        class MethodInjection {
            static class InjectMethodWithNoDependency {
                int called;
                @Inject
                public void install() {
                    called++;
                }
            }

            @Test
            public void should_invoke_inject_no_dependency_method() {
                config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);

                InjectMethodWithNoDependency component = config.getContext().get(InjectMethodWithNoDependency.class).get();
                assertEquals(1, component.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;
                @Inject
                public void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_via_method_with_dependency() {
                Dependency dependency = new Dependency() {};
                config.bind(Dependency.class, dependency);
                config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);

                InjectMethodWithDependency component = config.getContext().get(InjectMethodWithDependency.class).get();

                assertSame(dependency, component.dependency);
            }

            static class SuperclassInjectMethod {
                int superCalled;
                @Inject
                void install() {
                    superCalled++;
                }
            }
            static class SubclassInjectMethod extends SuperclassInjectMethod{
                int subCalled;
                @Inject
                void anotherInstall() {
                    subCalled = superCalled + 1;
                }
            }
            @Test
            public void should_inject_via_superclass_inject_method() {
                config.bind(SubclassInjectMethod.class, SubclassInjectMethod.class);

                SubclassInjectMethod component = config.getContext().get(SubclassInjectMethod.class).get();
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }


            static class SubclassInjectMethodWithInjectOverrideMethod extends SuperclassInjectMethod{
                @Inject
                void install() {
                    super.install();
                }
            }
            @Test
            public void should_invoke_subclass_inject_method_if_override_superclass_inject_method() {
                config.bind(SubclassInjectMethodWithInjectOverrideMethod.class, SubclassInjectMethodWithInjectOverrideMethod.class);

                SubclassInjectMethodWithInjectOverrideMethod component = config.getContext().get(SubclassInjectMethodWithInjectOverrideMethod.class).get();

                assertEquals(1, component.superCalled);
            }


            static class SubclassInjectMethodWithNoInjectOverrideMethod extends SuperclassInjectMethod{
                void install() {
                    super.install();
                }
            }
            @Test
            public void should_invoke_inject_method_if_subclass_override_method_is_not_injected() {
                config.bind(SubclassInjectMethodWithNoInjectOverrideMethod.class, SubclassInjectMethodWithNoInjectOverrideMethod.class);
                SubclassInjectMethodWithNoInjectOverrideMethod component = config.getContext().get(SubclassInjectMethodWithNoInjectOverrideMethod.class).get();
                assertEquals(0, component.superCalled);
            }

            @Test
            public void  should_include_dependencies_via_inject_method() {
                ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
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


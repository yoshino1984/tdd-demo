package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xiaoyi
 * 2023/1/2 18:27
 * @since
 **/
public class InjectionTest {

    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
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
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(ComponentWithMultiInjectedConstructors.class));
        }

        @Test
        public void should_throw_exception_when_class_no_injected_constructor_nor_default_constructor() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(ComponentWithNoInjectedNorDefaultConstructor.class));
        }

        abstract class AbstractComponent {
            @Inject
            public AbstractComponent() {
            }
        }

        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(ConstructorInjection.AbstractComponent.class));
        }

        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(Component.class));
        }

    }

    @Nested
    class FieldInjection {
        static class ComponentWithFieldInject {
            @Inject
            Dependency dependency;
        }

        static class ComponentWithFieldInjectSubclass extends FieldInjection.ComponentWithFieldInject {
        }

        @Test
        public void should_inject_via_field() {
            Dependency dependency = new Dependency() {
            };

            config.bind(Dependency.class, dependency);
            config.bind(FieldInjection.ComponentWithFieldInject.class, FieldInjection.ComponentWithFieldInject.class);

            FieldInjection.ComponentWithFieldInject component = config.getContext().get(FieldInjection.ComponentWithFieldInject.class).get();

            assertSame(dependency, component.dependency);
        }

        @Test
        public void should_inject_field_via_superclass() {
            Dependency dependency = new Dependency() {
            };

            config.bind(Dependency.class, dependency);
            config.bind(FieldInjection.ComponentWithFieldInjectSubclass.class, FieldInjection.ComponentWithFieldInjectSubclass.class);

            FieldInjection.ComponentWithFieldInjectSubclass component = config.getContext().get(FieldInjection.ComponentWithFieldInjectSubclass.class).get();

            assertSame(dependency, component.dependency);
        }

        @Test
        public void should_return_correct_dependencies_via_field_inject() {
            ConstructorInjectionComponentProvider<FieldInjection.ComponentWithFieldInjectSubclass> provider = new ConstructorInjectionComponentProvider<>(FieldInjection.ComponentWithFieldInjectSubclass.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
        }

        static class ComponentWithFinalFieldInject {
            @Inject
            final Dependency dependency = null;

        }

        @Test
        public void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(FieldInjection.ComponentWithFinalFieldInject.class));
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
            config.bind(MethodInjection.InjectMethodWithNoDependency.class, MethodInjection.InjectMethodWithNoDependency.class);

            MethodInjection.InjectMethodWithNoDependency component = config.getContext().get(MethodInjection.InjectMethodWithNoDependency.class).get();
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
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(MethodInjection.InjectMethodWithDependency.class, MethodInjection.InjectMethodWithDependency.class);

            MethodInjection.InjectMethodWithDependency component = config.getContext().get(MethodInjection.InjectMethodWithDependency.class).get();

            assertSame(dependency, component.dependency);
        }

        static class SuperclassInjectMethod {
            int superCalled;

            @Inject
            void install() {
                superCalled++;
            }
        }

        static class SubclassInjectMethod extends MethodInjection.SuperclassInjectMethod {
            int subCalled;

            @Inject
            void anotherInstall() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        public void should_inject_via_superclass_inject_method() {
            config.bind(MethodInjection.SubclassInjectMethod.class, MethodInjection.SubclassInjectMethod.class);

            MethodInjection.SubclassInjectMethod component = config.getContext().get(MethodInjection.SubclassInjectMethod.class).get();
            assertEquals(1, component.superCalled);
            assertEquals(2, component.subCalled);
        }


        static class SubclassInjectMethodWithInjectOverrideMethod extends MethodInjection.SuperclassInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_invoke_subclass_inject_method_if_override_superclass_inject_method() {
            config.bind(MethodInjection.SubclassInjectMethodWithInjectOverrideMethod.class, MethodInjection.SubclassInjectMethodWithInjectOverrideMethod.class);

            MethodInjection.SubclassInjectMethodWithInjectOverrideMethod component = config.getContext().get(MethodInjection.SubclassInjectMethodWithInjectOverrideMethod.class).get();

            assertEquals(1, component.superCalled);
        }


        static class SubclassInjectMethodWithNoInjectOverrideMethod extends MethodInjection.SuperclassInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_invoke_inject_method_if_subclass_override_method_is_not_injected() {
            config.bind(MethodInjection.SubclassInjectMethodWithNoInjectOverrideMethod.class, MethodInjection.SubclassInjectMethodWithNoInjectOverrideMethod.class);
            MethodInjection.SubclassInjectMethodWithNoInjectOverrideMethod component = config.getContext().get(MethodInjection.SubclassInjectMethodWithNoInjectOverrideMethod.class).get();
            assertEquals(0, component.superCalled);
        }

        @Test
        public void should_include_dependencies_via_inject_method() {
            ConstructorInjectionComponentProvider<MethodInjection.InjectMethodWithDependency> provider = new ConstructorInjectionComponentProvider<>(MethodInjection.InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

        // todo exception if include type parameter
        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        public void should_throw_exception_if_contain_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(MethodInjection.InjectMethodWithTypeParameter.class));
        }
    }
}

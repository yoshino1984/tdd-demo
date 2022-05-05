package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xiaoyi
 * 2022/5/4 15:21
 * @since
 **/
@Nested
public class InjectionTest {

    private ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    public class ConstructionInjection {

        static class ComponentWithDefaultConstructor implements Component {
            public ComponentWithDefaultConstructor() {
            }
        }

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

        static class DependencyWithInjectConstructor implements Dependency {
            private String dependency;

            @Inject
            DependencyWithInjectConstructor(String dependency) {
                this.dependency = dependency;
            }

            public String getDependency() {
                return dependency;
            }
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

        class ComponentWithMultiInjectConstructors implements Component {

            @Inject
            public ComponentWithMultiInjectConstructors(String name, Dependency dependency) {
            }

            @Inject
            public ComponentWithMultiInjectConstructors(String name) {
            }
        }

        @Test
        public void should_throw_exception_if_multi_inject_constructors() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectProvider<>(ComponentWithMultiInjectConstructors.class);
            });
        }

        class ComponentWithNoInjectNorDefaultConstructor implements Component {

            public ComponentWithNoInjectNorDefaultConstructor(String name) {
            }
        }

        @Test
        public void should_throw_exception_if_no_inject_nor_default_constructor() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectProvider<>(ComponentWithNoInjectNorDefaultConstructor.class);
            });
        }


        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent(Dependency dependency) {
            }
        }

        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(ConstructionInjection.AbstractComponent.class));
        }

        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(Component.class));
        }

    }

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection implements Component {
            @Inject
            Dependency dependency;
        }

        static class ComponentSubclassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {

        }

        @Test
        public void should_inject_dependency_via_field() {
            config.bind(Component.class, FieldInjection.ComponentWithFieldInjection.class);
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);

            Optional<Component> component = config.getContext().get(Component.class);

            assertSame(dependency, ((FieldInjection.ComponentWithFieldInjection) component.get()).dependency);
        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() {
            config.bind(Component.class, FieldInjection.ComponentSubclassWithFieldInjection.class);
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);

            Optional<Component> component = config.getContext().get(Component.class);

            assertSame(dependency, ((FieldInjection.ComponentSubclassWithFieldInjection) component.get()).dependency);
        }

        @Test
        public void should_include_field_dependency_from_field_inject() {
            ConstructorInjectProvider<FieldInjection.ComponentWithFieldInjection> provider = new ConstructorInjectProvider<>(FieldInjection.ComponentWithFieldInjection.class);
            assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
        }

        static class ComponentWithFinalFieldInjection implements Component {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        public void should_throw_exception_if_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(FieldInjection.ComponentWithFinalFieldInjection.class));
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
            config.bind(Component.class, MethodInjection.MethodInjectionWithNoDependency.class);

            Component component = config.getContext().get(Component.class).get();

            assertTrue(((MethodInjection.MethodInjectionWithNoDependency) component).called);
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
            config.bind(Component.class, MethodInjection.MethodInjectionInjectDependency.class);
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);

            Component component = config.getContext().get(Component.class).get();

            assertSame(dependency, ((MethodInjection.MethodInjectionInjectDependency) component).dependency);
        }

        @Test
        public void should_include_dependency_from_inject_method() {
            ConstructorInjectProvider<MethodInjection.MethodInjectionInjectDependency> provider = new ConstructorInjectProvider<>(MethodInjection.MethodInjectionInjectDependency.class);
            assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
        }

        static class MethodInjectionSuperclass implements Component {
            int called = 0;

            @Inject
            public void install() {
                called++;
            }

        }

        static class MethodInjectionSubclass extends MethodInjection.MethodInjectionSuperclass {
            int anotherCalled = 0;

            @Inject
            public void anotherInstall() {
                anotherCalled = called + 1;
            }
        }

        @Test
        public void should_call_superclass_inject_method_first_by_inject_subclass() {
            config.bind(Component.class, MethodInjection.MethodInjectionSubclass.class);

            MethodInjection.MethodInjectionSubclass component = (MethodInjection.MethodInjectionSubclass) config.getContext().get(Component.class).get();

            assertEquals(1, component.called);
            assertEquals(2, component.anotherCalled);
        }


        static class MethodInjectionSubclassOverrideMethod extends MethodInjection.MethodInjectionSuperclass {

            @Inject
            @Override
            public void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_subclass_override_method_via_inject_method() {
            config.bind(Component.class, MethodInjection.MethodInjectionSubclassOverrideMethod.class);


            MethodInjection.MethodInjectionSubclassOverrideMethod component = (MethodInjection.MethodInjectionSubclassOverrideMethod) config.getContext().get(Component.class).get();

            assertEquals(1, component.called);
        }


        static class MethodInjectionSubclassOverrideNoInjectMethod extends MethodInjection.MethodInjectionSuperclass {

            @Override
            public void install() {
                super.install();
            }
        }

        @Test
        public void should_not_invoke_no_inject_method_via_method() {
            config.bind(Component.class, MethodInjection.MethodInjectionSubclassOverrideNoInjectMethod.class);


            MethodInjection.MethodInjectionSubclassOverrideNoInjectMethod component = (MethodInjection.MethodInjectionSubclassOverrideNoInjectMethod) config.getContext().get(Component.class).get();

            assertEquals(0, component.called);
        }

        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {

            }
        }

        @Test
        public void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectProvider<>(MethodInjection.InjectMethodWithTypeParameter.class));
        }

    }
}

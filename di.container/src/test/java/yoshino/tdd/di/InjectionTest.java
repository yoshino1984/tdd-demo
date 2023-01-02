package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author xiaoyi
 * 2023/1/2 18:27
 * @since
 **/
public class InjectionTest {
    private Dependency dependency = mock(Dependency.class);

    private Context context = mock(Context.class);

    @BeforeEach
    public void setUp() {
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                ComponentWithDefaultConstructor instance = new ConstructorInjectionComponentProvider<>(ComponentWithDefaultConstructor.class).get(context);

                assertNotNull(instance);
            }

            @Test
            public void should_inject_dependency_from_constructor_dependency() {
                ComponentWithDependencyInjectedConstructor instance = new ConstructorInjectionComponentProvider<>(ComponentWithDependencyInjectedConstructor.class).get(context);

                assertNotNull(instance);
                assertEquals(dependency, instance.getDependency());
            }

            @Test
            public void should_include_dependency_via_inject_constructor() {
                ConstructorInjectionComponentProvider<ComponentWithDependencyInjectedConstructor> provider = new ConstructorInjectionComponentProvider<>(ComponentWithDependencyInjectedConstructor.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
            }
        }

        @Nested
        class IllegalInjectConstructors {

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
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(AbstractComponent.class));
            }

            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(Component.class));
            }
        }


    }

    @Nested
    class FieldInjection {

        @Nested
        class Injection {
            static class ComponentWithFieldInject {
                @Inject
                Dependency dependency;
            }

            static class ComponentWithFieldInjectSubclass extends ComponentWithFieldInject {
            }

            @Test
            public void should_inject_via_field() {

                ComponentWithFieldInject component = new ConstructorInjectionComponentProvider<>(ComponentWithFieldInject.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_field_via_superclass() {
                ComponentWithFieldInjectSubclass component = new ConstructorInjectionComponentProvider<>(ComponentWithFieldInjectSubclass.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_from_field_dependency() {
                ConstructorInjectionComponentProvider<ComponentWithFieldInjectSubclass> provider = new ConstructorInjectionComponentProvider<>(ComponentWithFieldInjectSubclass.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
            }
        }

        @Nested
        class IllegalInjectField {
            static class ComponentWithFinalFieldInject {
                @Inject
                final Dependency dependency = null;

            }

            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(ComponentWithFinalFieldInject.class));
            }
        }
    }

    @Nested
    class MethodInjection {
        @Nested
        class Injection {

            static class InjectMethodWithNoDependency {
                int called;

                @Inject
                public void install() {
                    called++;
                }
            }

            @Test
            public void should_invoke_inject_no_dependency_method() {


                InjectMethodWithNoDependency component = new ConstructorInjectionComponentProvider<>(InjectMethodWithNoDependency.class).get(context);
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
                InjectMethodWithDependency component = new ConstructorInjectionComponentProvider<>(InjectMethodWithDependency.class).get(context);

                assertSame(dependency, component.dependency);
            }

            static class SuperclassInjectMethod {
                int superCalled;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubclassInjectMethod extends SuperclassInjectMethod {
                int subCalled;

                @Inject
                void anotherInstall() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            public void should_inject_via_superclass_inject_method() {
                SubclassInjectMethod component = new ConstructorInjectionComponentProvider<>(SubclassInjectMethod.class).get(context);

                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }


            static class SubclassInjectMethodWithInjectOverrideMethod extends SuperclassInjectMethod {
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_invoke_subclass_inject_method_if_override_superclass_inject_method() {
                SubclassInjectMethodWithInjectOverrideMethod component = new ConstructorInjectionComponentProvider<>(SubclassInjectMethodWithInjectOverrideMethod.class).get(context);

                assertEquals(1, component.superCalled);
            }


            static class SubclassInjectMethodWithNoInjectOverrideMethod extends SuperclassInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_invoke_inject_method_if_subclass_override_method_is_not_injected() {
                SubclassInjectMethodWithNoInjectOverrideMethod component = new ConstructorInjectionComponentProvider<>(SubclassInjectMethodWithNoInjectOverrideMethod.class).get(context);

                assertEquals(0, component.superCalled);
            }

            @Test
            public void should_inject_dependency_from_method_dependency() {
                ConstructorInjectionComponentProvider<InjectMethodWithDependency> provider = new ConstructorInjectionComponentProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

        }

        @Nested
        class IllegalInjectMethods {
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            public void should_throw_exception_if_contain_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionComponentProvider<>(InjectMethodWithTypeParameter.class));
            }

        }


    }
}

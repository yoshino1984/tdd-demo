package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.lang.reflect.ParameterizedType;
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
    private Provider<Dependency> dependencyProvider = mock(Provider.class);
    private ParameterizedType dependencyType;

    private Context context = mock(Context.class);

    @BeforeEach
    public void setUp() throws NoSuchFieldException {
        dependencyType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));
        when(context.get(eq(ComponentRef.of(dependencyType)))).thenReturn(Optional.of(dependencyProvider));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                ComponentWithDefaultConstructor instance = new InjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);

                assertNotNull(instance);
            }

            @Test
            public void should_inject_dependency_from_constructor_dependency() {
                ComponentWithDependencyInjectedConstructor instance = new InjectionProvider<>(ComponentWithDependencyInjectedConstructor.class).get(context);

                assertNotNull(instance);
                assertEquals(dependency, instance.getDependency());
            }

            @Test
            public void should_include_dependency_via_inject_constructor() {
                InjectionProvider<ComponentWithDependencyInjectedConstructor> provider = new InjectionProvider<>(ComponentWithDependencyInjectedConstructor.class);

                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(new ComponentRef[0]));
            }

            @Test
            public void should_include_dependency_type_via_inject_constructor() {
                InjectionProvider<InjectProviderConstructor> provider = new InjectionProvider<>(InjectProviderConstructor.class);

                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyType)}, provider.getDependencies().toArray(new ComponentRef[0]));
            }

            static class InjectProviderConstructor {
                Provider<Dependency> dependencyProvider;

                @Inject
                public InjectProviderConstructor(Provider<Dependency> dependencyProvider) {
                    this.dependencyProvider = dependencyProvider;
                }
            }

            @Test
            public void should_inject_provider_via_inject_constructor() {
                InjectionProvider<InjectProviderConstructor> provider = new InjectionProvider<>(InjectProviderConstructor.class);
                assertSame(dependencyProvider, provider.get(context).dependencyProvider);
            }
        }

        @Nested
        class IllegalInjectConstructors {

            @Test
            public void should_throw_exception_when_class_with_multi_injected_constructor() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithMultiInjectedConstructors.class));
            }

            @Test
            public void should_throw_exception_when_class_no_injected_constructor_nor_default_constructor() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithNoInjectedNorDefaultConstructor.class));
            }

            abstract class AbstractComponent {
                @Inject
                public AbstractComponent() {
                }
            }

            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(AbstractComponent.class));
            }

            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(TestComponent.class));
            }
        }

        @Nested
        public class WithQualifier {

            @BeforeEach
            void before() {
                reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("choseOne"))))).thenReturn(Optional.of(dependency));
            }

            static class InjectConstructor {
                private Dependency dependency;
                @Inject
                public InjectConstructor(@Named("choseOne") Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_with_qualifier_via_constructor() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                InjectConstructor component = provider.get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);

                assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new NamedLiteral("choseOne"))},
                    provider.getDependencies().toArray(new ComponentRef[0]));
            }

            static class MultiQualifiersInjectConstructor {
                @Inject
                public MultiQualifiersInjectConstructor(@Named("choseOne") @Skywalker Dependency dependency) {
                }
            }
            @Test
            public void should_throw_exception_if_multi_qualifiers_given() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifiersInjectConstructor.class));
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

                ComponentWithFieldInject component = new InjectionProvider<>(ComponentWithFieldInject.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_field_via_superclass() {
                ComponentWithFieldInjectSubclass component = new InjectionProvider<>(ComponentWithFieldInjectSubclass.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_from_field_dependency() {
                InjectionProvider<ComponentWithFieldInjectSubclass> provider = new InjectionProvider<>(ComponentWithFieldInjectSubclass.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(new ComponentRef[0]));

            }

            @Test
            public void should_include_dependency_type_via_inject_field() {
                InjectionProvider<InjectProviderField> provider = new InjectionProvider<>(InjectProviderField.class);

                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyType)}, provider.getDependencies().toArray(new ComponentRef[0]));
            }

            static class InjectProviderField {
                @Inject
                Provider<Dependency> dependencyProvider;
            }

            @Test
            public void should_inject_provider_via_inject_field() {
                InjectionProvider<InjectProviderField> provider = new InjectionProvider<>(InjectProviderField.class);
                assertSame(dependencyProvider, provider.get(context).dependencyProvider);
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
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithFinalFieldInject.class));
            }
        }

        @Nested
        public class WithQualifier {

            @BeforeEach
            void before() {
                reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("choseOne"))))).thenReturn(Optional.of(dependency));
            }

            static class InjectField {
                @Inject
                @Named("choseOne")
                Dependency dependency;
            }

            @Test
            public void should_inject_dependency_with_qualifier_via_field() {
                InjectionProvider<InjectField> provider = new InjectionProvider<>(InjectField.class);
                InjectField component = provider.get(context);

                assertSame(dependency, component.dependency);
            }


            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectField> provider = new InjectionProvider<>(InjectField.class);

                assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new NamedLiteral("choseOne"))},
                    provider.getDependencies().toArray(new ComponentRef[0]));
            }

            static class MultiQualifiersInjectField {
                @Inject
                @Named("choseOne") @Skywalker Dependency dependency;
            }
            @Test
            public void should_throw_exception_if_multi_qualifiers_given() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifiersInjectField.class));
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


                InjectMethodWithNoDependency component = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
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
                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);

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
                SubclassInjectMethod component = new InjectionProvider<>(SubclassInjectMethod.class).get(context);

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
                SubclassInjectMethodWithInjectOverrideMethod component = new InjectionProvider<>(SubclassInjectMethodWithInjectOverrideMethod.class).get(context);

                assertEquals(1, component.superCalled);
            }


            static class SubclassInjectMethodWithNoInjectOverrideMethod extends SuperclassInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_invoke_inject_method_if_subclass_override_method_is_not_injected() {
                SubclassInjectMethodWithNoInjectOverrideMethod component = new InjectionProvider<>(SubclassInjectMethodWithNoInjectOverrideMethod.class).get(context);

                assertEquals(0, component.superCalled);
            }

            @Test
            public void should_inject_dependency_from_method_dependency() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(new ComponentRef[0]));
            }

            @Test
            public void should_include_dependency_type_via_inject_method() {
                InjectionProvider<InjectProviderMethod> provider = new InjectionProvider<>(InjectProviderMethod.class);

                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyType)}, provider.getDependencies().toArray(new ComponentRef[0]));
            }


            static class InjectProviderMethod {
                Provider<Dependency> dependencyProvider;

                @Inject
                public void install(Provider<Dependency> dependencyProvider) {
                    this.dependencyProvider = dependencyProvider;
                }
            }

            @Test
            public void should_inject_provider_via_inject_method() {
                InjectionProvider<InjectProviderMethod> provider = new InjectionProvider<>(InjectProviderMethod.class);
                assertSame(dependencyProvider, provider.get(context).dependencyProvider);
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
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }

        }

        @Nested
        public class WithQualifier {

            @BeforeEach
            void before() {
                reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("choseOne"))))).thenReturn(Optional.of(dependency));
            }

            static class InjectMethod {
                Dependency dependency;
                @Inject
                public void install(@Named("choseOne") Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_with_qualifier_via_method() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);
                InjectMethod component = provider.get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);

                assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new NamedLiteral("choseOne"))},
                    provider.getDependencies().toArray(new ComponentRef[0]));
            }

            static class MultiQualifiersInjectMethod {
                @Inject
                public void install(@Named("choseOne") @Skywalker Dependency dependency) {
                }
            }

            @Test
            public void should_throw_exception_if_multi_qualifiers_given() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifiersInjectMethod.class));
            }
        }


    }
}

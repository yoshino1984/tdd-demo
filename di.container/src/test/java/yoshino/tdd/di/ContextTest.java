package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xiaoyi
 * 2023/1/4 00:33
 * @since
 **/
@Nested
public class ContextTest {

    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    class BindingType {

        @Test
        public void should_bind_type_to_a_specific_instance() {
            TestComponent instance = new TestComponent() {
            };

            config.bind(TestComponent.class, instance);

            assertEquals(instance, config.getContext().get(ComponentRef.of(TestComponent.class)).get());
        }

        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));

            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Provider<TestComponent> provider = config.getContext().get(new ComponentRef<Provider<TestComponent>>(){}).get();

            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_for_unsupported_type() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            assertFalse(config.getContext().get(new ComponentRef<List<TestComponent>>() {}).isPresent());
        }

        @Nested
        public class WithQualifier {

            @Test
            public void should_bind_instance_with_multi_qualifiers() {
                TestComponent instance = new TestComponent() {
                };
                config.bind(TestComponent.class, instance, new NamedLiteral("choseOne"), new SkywalkerLiteral());

                Context context = config.getContext();
                Optional<TestComponent> choseOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("choseOne")));
                Optional<TestComponent> skyWalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()));

                assertTrue(choseOne.isPresent());
                assertTrue(skyWalker.isPresent());
                assertSame(instance, choseOne.get());
                assertSame(instance, skyWalker.get());
            }

            @Test
            public void should_bind_component_with_multi_qualifiers() {

                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(TestComponent.class, ComponentWithDefaultConstructor.class, new NamedLiteral("choseOne"), new SkywalkerLiteral());


                Context context = config.getContext();
                Optional<TestComponent> choseOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("choseOne")));
                Optional<TestComponent> skyWalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()));

                assertTrue(choseOne.isPresent());
                assertTrue(skyWalker.isPresent());
                assertTrue(choseOne.get() instanceof ComponentWithDefaultConstructor);
                assertTrue(skyWalker.get() instanceof ComponentWithDefaultConstructor);
            }

            // todo throw illegal component if qualifier illegal
            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent instance = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, instance, new TestLiteral(), new SkywalkerLiteral()));
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, ComponentWithDefaultConstructor.class, new TestLiteral(), new SkywalkerLiteral()));
            }

            // todo provider
        }


    }

    @Nested
    class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> type) {
            config.bind(TestComponent.class, type);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

            assertSame(exception.getDependency().type(), Dependency.class);
        }

        private static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("inject constructor", MissingDependencyConstructor.class)),
                Arguments.of(Named.of("inject method", MissingDependencyMethod.class)),
                Arguments.of(Named.of("inject field", MissingDependencyField.class)),
                Arguments.of(Named.of("provider in inject constructor", MissingProviderDependencyConstructor.class)),
                Arguments.of(Named.of("provider in inject field", MissingProviderDependencyField.class)),
                Arguments.of(Named.of("provider in inject method", MissingProviderDependencyMethod.class)));
        }


        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            public void install(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            private Dependency dependency;
        }

        static class MissingProviderDependencyConstructor implements TestComponent {
            @Inject
            public MissingProviderDependencyConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingProviderDependencyField implements TestComponent {
            @Inject
            private Provider<Dependency> dependency;
        }

        static class MissingProviderDependencyMethod implements TestComponent {
            @Inject
            private void install(Provider<Dependency> dependency) {
            }
        }

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_exist_cyclic_dependencies(Class<? extends TestComponent> component,
                                                                        Class<? extends Dependency> dependency) {
            config.bind(TestComponent.class, ComponentWithDependencyInjectedConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnComponent.class);

            CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class, () -> config.getContext());

            assertTrue(exception.getComponents().contains(TestComponent.class));
            assertTrue(exception.getComponents().contains(Dependency.class));
        }

        private static Stream<Arguments> should_throw_exception_if_exist_cyclic_dependencies() {
            List<Arguments> result = new ArrayList<>();
            for (Named<? extends Class<? extends TestComponent>> component : List.of(
                Named.of("inject constructor", DependencyCheck.ComponentInjectConstructor.class),
                Named.of("inject field", DependencyCheck.ComponentInjectField.class),
                Named.of("inject method", DependencyCheck.ComponentInjectMethod.class))) {
                for (Named<? extends Class<? extends Dependency>> dependency : List.of(
                    Named.of("inject constructor", DependencyCheck.CyclicDependencyInjectConstructor.class),
                    Named.of("inject field", DependencyCheck.CyclicDependencyInjectField.class),
                    Named.of("inject method", DependencyCheck.CyclicDependencyInjectMethod.class))) {
                    result.add(Arguments.of(component, dependency));
                }
            }
            return result.stream();
        }

        static class ComponentInjectConstructor implements TestComponent {
            private Dependency dependency;

            @Inject
            public ComponentInjectConstructor(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        class ComponentInjectField implements TestComponent {
            @Inject
            private Dependency dependency;

            public ComponentInjectField() {
            }
        }

        class ComponentInjectMethod implements TestComponent {
            private Dependency dependency;

            public ComponentInjectMethod() {
            }

            @Inject
            public void install(Dependency dependency) {
            }
        }

        class CyclicDependencyInjectConstructor implements Dependency {
            private TestComponent component;

            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        class CyclicDependencyInjectField implements Dependency {
            @Inject
            private TestComponent component;

            public CyclicDependencyInjectField() {
            }
        }

        class CyclicDependencyInjectMethod implements Dependency {

            public CyclicDependencyInjectMethod() {
            }

            @Inject
            public void install(TestComponent component) {
            }
        }


        @Test
        public void should_throw_exception_if_exist_transitive_cyclic_dependencies() {
            config.bind(TestComponent.class, ComponentWithDependencyInjectedConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

            CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class, () -> config.getContext());

            assertTrue(exception.getComponents().contains(TestComponent.class));
            assertTrue(exception.getComponents().contains(Dependency.class));
            assertTrue(exception.getComponents().contains(AnotherDependency.class));
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, ComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Optional<TestComponent> instance = config.getContext().get(ComponentRef.of(TestComponent.class));
            assertTrue(instance.isPresent());
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> dependency) {
            }
        }

        @Nested
        public class WithQualifier {
            // todo dependency missing if qualifier not match
            static class DependencyWithNamedQualifier {
                @Inject
                public DependencyWithNamedQualifier(@Skywalker Dependency dependency) {
                }
            }

            @Test
            public void should_throw_exception_if_dependency_with_qualifier_missing() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(DependencyWithNamedQualifier.class, DependencyWithNamedQualifier.class, new NamedLiteral("choseOne"));

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
                assertEquals(new Component(DependencyWithNamedQualifier.class, new NamedLiteral("choseOne")), exception.getComponent());
            }

            static class SkywalkerDependency implements Dependency {
                @Inject
                public SkywalkerDependency(@Skywalker Dependency dependency) {
                }
            }

            static class NotCyclicDependency implements Dependency {
                @Inject
                public NotCyclicDependency(@jakarta.inject.Named("choseOne") Dependency dependency) {
                }
            }

            @Test
            public void should_not_throw_exception_if_component_with_same_type_taged_with_different_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency, new SkywalkerLiteral());
                config.bind(Dependency.class, SkywalkerDependency.class, new NamedLiteral("choseOne"));
                config.bind(Dependency.class, NotCyclicDependency.class);

                assertDoesNotThrow(() -> config.getContext());
            }

        }
    }

}

record NamedLiteral(String value) implements jakarta.inject.Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof jakarta.inject.Named named) {
            return Objects.equals(value, named.value());
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Annotation hashCode rule
        return "value".hashCode() * 127 ^ value.hashCode();
    }
}

@Qualifier
@Documented
@Retention(RUNTIME)
@interface Skywalker {

}

record SkywalkerLiteral() implements Skywalker {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Skywalker.class;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Skywalker;
    }
}
record TestLiteral() implements Test {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}
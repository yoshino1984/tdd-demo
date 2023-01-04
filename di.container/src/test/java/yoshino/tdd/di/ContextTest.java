package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
            Component instance = new Component() {
            };

            config.bind(Component.class, instance);

            assertEquals(instance, config.getContext().get(Component.class).get());
        }

        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Optional<Component> component = config.getContext().get(Component.class);

            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            ParameterizedType type = new TypeLiteral<Provider<Component>>() {
            }.getType();
            Provider<Component> provider = (Provider<Component>) config.getContext().get(type).get();

            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_for_unsupported_type() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            ParameterizedType type = new TypeLiteral<List<Component>>() {
            }.getType();
            assertFalse(config.getContext().get(type).isPresent());
        }

        abstract class TypeLiteral<T> {
            public ParameterizedType getType() {
                return (ParameterizedType) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            }
        }

    }

    @Nested
    class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> type) {
            config.bind(Component.class, type);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

            assertSame(exception.getInstance(), Dependency.class);
        }

        private static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("inject constructor", MissingDependencyConstructor.class)),
                Arguments.of(Named.of("inject method", MissingDependencyMethod.class)),
                Arguments.of(Named.of("inject field", MissingDependencyField.class)),
                Arguments.of(Named.of("provider in inject constructor", MissingProviderDependencyConstructor.class)),
                Arguments.of(Named.of("provider in inject field", MissingProviderDependencyField.class)),
                Arguments.of(Named.of("provider in inject method", MissingProviderDependencyMethod.class)));
        }


        static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            public void install(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            private Dependency dependency;
        }

        static class MissingProviderDependencyConstructor implements Component {
            @Inject
            public MissingProviderDependencyConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingProviderDependencyField implements Component {
            @Inject
            private Provider<Dependency> dependency;
        }

        static class MissingProviderDependencyMethod implements Component {
            @Inject
            private void install(Provider<Dependency> dependency) {
            }
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

        static class ComponentInjectConstructor implements Component {
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
            public void install(Dependency dependency) {
            }
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
            public void install(Component component) {
            }
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

        // todo should not throw exception if cyclic dependency via provider
        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(Component.class, ComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Optional<Component> instance = config.getContext().get(Component.class);
            assertTrue(instance.isPresent());
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<Component> dependency) {
            }
        }
    }

}

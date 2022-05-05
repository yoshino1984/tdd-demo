package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import yoshino.tdd.di.exception.CyclicDependenciesException;
import yoshino.tdd.di.exception.DependencyNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xiaoyi
 * 2022/5/5 23:26
 * @since
 **/
@Nested
public class ContextTest {

    private ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBinding {
        static class ConstructorInjection implements Component {
            private Dependency dependency;

            @Inject
            ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements Component {
            @Inject
            private Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
            private Dependency dependency;

            @Inject
            public void setDependency(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            Optional<Component> component = config.getContext().get(Component.class);

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());

        }

        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class)),
                Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)));
        }


        @Test
        public void should_bind_type_to_an_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            assertEquals(instance, config.getContext().get(Component.class).get());
        }

        @Test
        public void should_retrieve_empty_for_unbind_type() {
            assertTrue(config.getContext().get(Component.class).isEmpty());
        }

    }

    @Nested
    public class DependencyCheck {

        static class ComponentConstructorInjectionDependOnDependency implements Component {
            @Inject
            ComponentConstructorInjectionDependOnDependency(Dependency dependency) {
            }
        }

        static class ComponentFieldInjectionWithDependOnDependency implements Component {
            @Inject
            private Dependency dependency;
        }

        static class ComponentMethodInjectionDependOnDependency implements Component {
            @Inject
            public void setDependency(Dependency dependency) {
            }
        }

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> componentType) {
            config.bind(Component.class, componentType);
            DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext().get(Component.class).get();
            });

            assertEquals(Dependency.class, e.getDependency());
            assertEquals(Component.class, e.getComponent());
        }

        static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("constructor inject dependency not found", ComponentConstructorInjectionDependOnDependency.class)),
                Arguments.of(Named.of("field inject dependency not found", ComponentFieldInjectionWithDependOnDependency.class)),
                Arguments.of(Named.of("method inject dependency not found", ComponentMethodInjectionDependOnDependency.class)));
        }

        static class DependencyConstructorInjectionDependOnComponent implements Dependency {
            @Inject
            DependencyConstructorInjectionDependOnComponent(Component component) {
            }
        }

        static class DependencyFieldInjectionWithDependOnComponent implements Dependency {
            @Inject
            private Component component;
        }

        static class DependencyMethodInjectionDependOnComponent implements Dependency {
            @Inject
            public void setDependency(Component component) {
            }
        }

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component,
                                                                        Class<? extends Dependency> dependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            CyclicDependenciesException e = assertThrows(CyclicDependenciesException.class, () -> {
                config.getContext();
            });

            List<Class<?>> components = e.getComponents();

            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
        }

        static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            Map<String, Class<?>> componentMap = Map.of("constructor", ComponentConstructorInjectionDependOnDependency.class,
                "field", ComponentFieldInjectionWithDependOnDependency.class,
                "method", ComponentMethodInjectionDependOnDependency.class);
            Map<String, Class<?>> dependencyMap = Map.of("constructor", DependencyConstructorInjectionDependOnComponent.class,
                "field", DependencyFieldInjectionWithDependOnComponent.class,
                "method", DependencyMethodInjectionDependOnComponent.class);

            List<Arguments> result = new ArrayList<>();
            componentMap.forEach((componentKey, componentValue) ->
                dependencyMap.forEach((dependencyKey, dependencyValue) ->
                    result.add(Arguments.of(Named.of(componentKey, componentValue), Named.of(dependencyKey, dependencyValue)))));

            return result.stream();
        }

        static class DependencyDependOnAnotherDependencyConstructor implements Dependency {
            private AnotherDependency dependency;

            @Inject
            DependencyDependOnAnotherDependencyConstructor(AnotherDependency dependency) {
                this.dependency = dependency;
            }
        }
        static class DependencyDependOnAnotherDependencyField implements Dependency {
            @Inject
            private AnotherDependency dependency;
        }
        static class DependencyDependOnAnotherDependencyMethod implements Dependency {

            @Inject
            public void setDependency(AnotherDependency dependency) {
            }
        }

        static class AnotherDependencyDependOnComponentConstructor implements AnotherDependency {
            private Component component;

            @Inject
            AnotherDependencyDependOnComponentConstructor(Component component) {

            }
        }
        static class AnotherDependencyDependOnComponentField implements AnotherDependency {
            @Inject
            private Component component;
        }
        static class AnotherDependencyDependOnComponentMethod implements AnotherDependency {
            private Component component;

            @Inject
            public void setComponent(Component component) {
                this.component = component;
            }
        }


        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends Component> componentType,
                                                                                   Class<? extends Dependency> dependencyType,
                                                                                   Class<? extends AnotherDependency> anotherDependencyType) {
            config.bind(Component.class, componentType);
            config.bind(Dependency.class, dependencyType);
            config.bind(AnotherDependency.class, anotherDependencyType);
            CyclicDependenciesException e = assertThrows(CyclicDependenciesException.class, () -> {
                config.getContext();
            });

            List<Class<?>> components = e.getComponents();

            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

        static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            Map<String, Class<?>> componentMap = Map.of("constructor", ComponentConstructorInjectionDependOnDependency.class,
                "field", ComponentFieldInjectionWithDependOnDependency.class,
                "method", ComponentMethodInjectionDependOnDependency.class);
            Map<String, Class<?>> dependencyMap = Map.of("constructor", DependencyDependOnAnotherDependencyConstructor.class,
                "field", DependencyDependOnAnotherDependencyField.class,
                "method", DependencyDependOnAnotherDependencyMethod.class);
            Map<String, Class<?>> anotherDependencyMap = Map.of("constructor", AnotherDependencyDependOnComponentConstructor.class,
                "field", AnotherDependencyDependOnComponentField.class,
                "method", AnotherDependencyDependOnComponentMethod.class);

            List<Arguments> result = new ArrayList<>();
            componentMap.forEach((componentKey, componentValue) ->
                dependencyMap.forEach((dependencyKey, dependencyValue) ->
                    anotherDependencyMap.forEach((anotherKey, anotherValue) ->
                    result.add(Arguments.of(Named.of(componentKey, componentValue), Named.of(dependencyKey, dependencyValue), Named.of(anotherKey, anotherValue))))));

            return result.stream();
        }
    }

}

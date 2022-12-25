package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;


public class ContainerTest {

    @Nested
    public class ComponentConstruction {

        Context context;

        @BeforeEach
        public void setUp() {
            context = new Context();
        }

        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };

            context.bind(Component.class, instance);

            assertEquals(instance, context.get(Component.class));
        }

        // todo abstract class
        // todo interface

        @Nested
        public class ConstructorInjection {

            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class);

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            @Test
            public void should_bind_type_to_a_class_with_dependency_injected() {
                Dependency dependency = new Dependency() {
                };

                context.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);
                context.bind(Dependency.class, dependency);

                Component instance = context.get(Component.class);
                assertNotNull(instance);
                assertEquals(dependency, ((ComponentWithDependencyInjectedConstructor) instance).getDependency());
            }

            public void should_bind_type_to_a_class_with_transitive_dependencies_injected() {
                context.bind(Component.class, ComponentWithDependencyInjectedConstructor.class);
                context.bind(Dependency.class, DependencyWithDependencyInjected.class);
                context.bind(String.class, "injected dependencies");

                Component instance = context.get(Component.class);
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithDependencyInjectedConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("injected dependencies", ((DependencyWithDependencyInjected)dependency).getName());
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

class ComponentWithDefaultConstructor implements Component {

    public ComponentWithDefaultConstructor() {
    }
}

interface Dependency {
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

class DependencyWithDependencyInjected implements Dependency {
    private String name;

    @Inject
    DependencyWithDependencyInjected(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}


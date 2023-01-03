package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class ContainerTest {

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


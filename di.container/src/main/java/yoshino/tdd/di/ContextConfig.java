package yoshino.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import yoshino.tdd.di.exception.CyclicDependenciesException;
import yoshino.tdd.di.exception.DependencyNotFoundException;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author xiaoyi
 * 2022/12/25 16:21
 * @since
 **/
public class ContextConfig {
    private Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

    public ContextConfig() {
        scopes.put(Singleton.class, SingletonProvider::new);
    }

    public <Type> void instance(Class<Type> type, Type instance) {
        bindInstance(type, instance, null);
    }


    public <Type> void instance(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            bindInstance(type, instance, qualifier);
        }
    }

    private void bindInstance(Class<?> type, Object instance, Annotation qualifier) {
        components.put(new Component(type, qualifier), (ComponentProvider<?>) context -> instance);
    }

    public <Type, Implementation extends Type>
    void component(Class<Type> type, Class<Implementation> implementation) {
        Annotation[] scopes = Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).toArray(Annotation[]::new);
        component(type, implementation, scopes);
    }

    public <Type, Implementation extends Type>
    void component(Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {
        bindComponent(type, implementation, annotations);
    }

    private void bindComponent(Class<?> type, Class<?> implementation, Annotation... annotations) {
        Map<? extends Class<? extends Annotation>, List<Annotation>> annotationGroups = Arrays.stream(annotations).collect(Collectors.groupingBy(this::typeOf, Collectors.toList()));

        if (annotationGroups.containsKey(Illegal.class)) {
            throw new IllegalComponentException();
        }

        bind(type, annotationGroups.getOrDefault(Qualifier.class, List.of()),
            createScopeProvider(implementation, annotationGroups.getOrDefault(Scope.class, List.of())));
    }

    private <Type> void bind(Class<Type> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    public void from(Config config) {
        new DSL(config).bind();
    }

    private <Type> ComponentProvider<?> createScopeProvider(Class<Type> implementation, List<Annotation> scopes) {
        if (scopes.size() > 1) {
            throw new IllegalComponentException();
        }
        InjectionProvider<?> injectionProvider = new InjectionProvider<>(implementation);
        return scopes.stream().findFirst().or(() -> scopeFrom(implementation)).<ComponentProvider<?>>map(s -> getScopeProvider(injectionProvider, s))
            .orElse(injectionProvider);
    }

    private static <Type> Optional<Annotation> scopeFrom(Class<Type> implementation) {
        return Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();
    }

    private Class<? extends Annotation> typeOf(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        return Stream.of(Qualifier.class, Scope.class).filter(type::isAnnotationPresent).findFirst().orElse(Illegal.class);
    }

    @interface Illegal {
    }

    private ComponentProvider<?> getScopeProvider(InjectionProvider<?> injectionProvider, Annotation scope) {
        if (!scopes.containsKey(scope.annotationType())) {
            throw new IllegalComponentException();
        }
        return scopes.get(scope.annotationType()).create(injectionProvider);
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scope, ScopeProvider scopeProvider) {
        scopes.put(scope, scopeProvider);
    }

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> componentRef) {
                if (componentRef.isContainer()) {
                    if (componentRef.getContainer() != Provider.class) {
                        return Optional.empty();
                    }
                    return (Optional<ComponentType>) Optional.ofNullable(components.get(componentRef.component())).map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(components.get(componentRef.component())).map(it -> (ComponentType) it.get(this));
            }
        };
    }

    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFoundException(component, dependency.component());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) {
                    throw new CyclicDependenciesException(visiting);
                }
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }

    private class DSL {
        private Config config;

        public DSL(Config config) {
            this.config = config;
        }

        public void bind() {
            try {
                List<Field> fields = Arrays.stream(config.getClass().getDeclaredFields()).toList();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(config);
                    Class<?> type = Arrays.stream(field.getAnnotations()).filter(a -> a.annotationType() == Config.Export.class)
                        .<Class<?>>map(a -> ((Config.Export) a).value()).findFirst()
                        .orElse(field.getType());
                    Annotation qualifier = Arrays.stream(field.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                        .findFirst().orElse(null);
                    if (value != null) {
                        ContextConfig.this.bindInstance(type, value, qualifier);
                    } else {
                        if (qualifier == null) {
                            ContextConfig.this.bindComponent(type, field.getType());
                        } else {
                            ContextConfig.this.bindComponent(type, field.getType(), qualifier);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}

class ContextConfigError extends Error {

    public static ContextConfigError unsatisfiedResolution(Component component, Component dependency) {
        return new ContextConfigError(MessageFormat.format("Unsatisfied resolution: {1} for {0}", component, dependency));
    }

    public static ContextConfigError circularDependencies(Collection<Component> path, Component circular) {
        return new ContextConfigError(MessageFormat.format("Circular resolution: {0} -> [{1}]",
            path.stream().map(Objects::toString).collect(Collectors.joining(" -> ")), circular));
    }

    public ContextConfigError(String message) {
        super(message);
    }
}

class ContextConfigException extends RuntimeException {

    static ContextConfigException illegalAnnotation(Class<?> type, List<Annotation> annotations) {
        return new ContextConfigException(MessageFormat.format("Unqualified annotations: {0} for {1}",
            String.join(" , ", annotations.stream().map(Objects::toString).toList()), type));
    }

    static ContextConfigException unknownScope(Class<? extends Annotation> annotationType) {
        return new ContextConfigException(MessageFormat.format("Unknown scope: {0}", annotationType));
    }

    static ContextConfigException duplicated(Class<? extends Annotation> annotationType) {
        return new ContextConfigException(MessageFormat.format("Duplicated: {0}", annotationType));
    }

    public ContextConfigException(String message) {
        super(message);
    }
}


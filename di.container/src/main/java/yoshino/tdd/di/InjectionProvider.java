package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

/**
 * @author xiaoyi
 * 2022/12/31 13:22
 * @since
 **/
class InjectionProvider<T> implements ComponentProvider<T> {
    private Constructor<?> injectConstructor;

    private List<Field> injectFields;

    private List<Method> injectMethods;
    private List<ComponentRef> dependencies;

    public InjectionProvider(Class<T> componentType) {
        if (Modifier.isAbstract(componentType.getModifiers())) {
            throw new IllegalComponentException();
        }

        this.injectConstructor = getInjectConstructor(componentType);
        this.injectFields = getInjectFields(componentType);
        this.injectMethods = getInjectMethods(componentType);

        if (injectFields.stream().anyMatch(it -> Modifier.isFinal(it.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(it -> it.getTypeParameters().length > 0)) {
            throw new IllegalComponentException();
        }
        this.dependencies = getDependencies();
    }

    @Override
    public T get(Context context) {
        try {
            T result = (T) injectConstructor.newInstance(toDependencies(context, injectConstructor));
            for (Field field : injectFields) {
                field.setAccessible(true);
                field.set(result, toDependency(context, field));
            }
            for (Method method : injectMethods) {
                method.invoke(result, toDependencies(context, method));
            }
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef> getDependencies() {
        return concat(concat(stream(injectConstructor.getParameters()).map(InjectionProvider::toComponentRef),
                injectFields.stream().map(f -> toComponentRef(f))),
            injectMethods.stream().flatMap(p -> stream(p.getParameters()).map(InjectionProvider::toComponentRef)))
            .toList();
    }

    private static ComponentRef toComponentRef(Field f) {
        return ComponentRef.of(f.getGenericType(), getQualifier(f));
    }


    private static ComponentRef<?> toComponentRef(Parameter p) {
        return ComponentRef.of(p.getParameterizedType(), getQualifier(p));
    }

    private static Annotation getQualifier(AnnotatedElement element) {
        List<Annotation> qualifiers = getQualifiers(element);
        if (qualifiers.size() > 1) {
            throw new IllegalComponentException();
        }
        return qualifiers.stream().findFirst().orElse(null);
    }

    private static List<Annotation> getQualifiers(AnnotatedElement f) {
        return stream(f.getAnnotations())
            .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
    }

    private static <Type, Implementation extends Type> Constructor<?> getInjectConstructor(Class<Implementation> implementation) {
        List<Constructor<?>> injectedConstructors = injectable(implementation.getConstructors()).toList();
        if (injectedConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return injectedConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
    }

    private List<Field> getInjectFields(Class<T> componentType) {
        return traverse(componentType, (injectFields1, current) -> injectable(current.getDeclaredFields()).toList());
    }

    private List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
            .filter(m -> isOverrideByInjectMethod(methods, m))
            .filter(m -> isOverrideByNoInjectMethod(component, m)).toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<T> traverse(Class<?> componentType, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> result = new ArrayList<>();
        Class<?> current = componentType;
        while (current != Object.class) {
            result.addAll(finder.apply(result, current));
            current = current.getSuperclass();
        }
        return result;
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, field.getGenericType(), getQualifier(field));
    }

    private static Object[] toDependencies(Context context, Executable constructor) {
        return stream(constructor.getParameters()).map(p -> toDependency(context, p.getParameterizedType(), getQualifier(p))).toArray(Object[]::new);
    }

    private static Object toDependency(Context context, Type type, Annotation qualifier) {
        return context.get(ComponentRef.of(type, qualifier)).get();
    }

    private static boolean isOverrideByNoInjectMethod(Class<?> componentType, Method m) {
        return isOverrideByNoInjectMethod(m, componentType.getDeclaredMethods());
    }

    private static boolean isOverrideByNoInjectMethod(Method m, Method[] declaredMethods) {
        return stream(declaredMethods).filter(it -> !it.isAnnotationPresent(Inject.class)).noneMatch(it -> isOverride(m, it));
    }

    private static boolean isOverrideByInjectMethod(List<Method> result, Method m) {
        return result.stream().noneMatch(it -> isOverride(m, it));
    }

    private static boolean isOverride(Method m, Method it) {
        return it.getName().equals(m.getName()) && Arrays.equals(it.getParameterTypes(), m.getParameterTypes());
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] fields) {
        return stream(fields).filter(it -> it.isAnnotationPresent(Inject.class));
    }

    private static <Type, Implementation extends Type> Constructor<Implementation> defaultConstructor(Class<Implementation> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }
}

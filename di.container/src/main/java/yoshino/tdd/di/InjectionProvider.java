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
    private Injectable<Constructor<?>> injectConstructor;
    private List<Injectable<Method>> injectMethods;
    private List<Injectable<Field>> injectFields;

    public InjectionProvider(Class<T> componentType) {
        if (Modifier.isAbstract(componentType.getModifiers())) {
            throw new IllegalComponentException();
        }

        this.injectConstructor = getInjectConstructor(componentType);
        this.injectMethods = getInjectMethods(componentType);
        this.injectFields = getInjectFields(componentType);

        if (injectFields.stream().map(Injectable::element).anyMatch(it -> Modifier.isFinal(it.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().map(Injectable::element).anyMatch(it -> it.getTypeParameters().length > 0)) {
            throw new IllegalComponentException();
        }
    }

    @Override
    public T get(Context context) {
        try {
            T result = (T) injectConstructor.element().newInstance(injectConstructor.toDependencies(context));

            for (Injectable<Field> field : injectFields) {
                field.element().setAccessible(true);
                field.element().set(result, field.toDependency(context));
            }

            for (Injectable<Method> method : injectMethods) {
                method.element().invoke(result, method.toDependencies(context));
            }
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return concat(concat(Stream.of(injectConstructor), injectFields.stream()), injectMethods.stream())
            .flatMap(m -> stream(m.required())).toList();
    }

    record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {

        private static <T extends Executable> Injectable<T> of(T element) {
            return new Injectable<>(element, stream(element.getParameters()).map(Injectable::toComponentRef).toArray(ComponentRef<?>[]::new));
        }

        public static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef[]{toComponentRef(field)});
        }

        public Object[] toDependencies(Context context) {
            return stream(required()).map(context::get).map(Optional::get).toArray(Object[]::new);
        }

        public Object toDependency(Context context) {
            return context.get(required()[0]).get();
        }

        private static ComponentRef<?> toComponentRef(Field f) {
            return ComponentRef.of(f.getGenericType(), getQualifier(f));
        }

        private static ComponentRef<?> toComponentRef(Parameter p) {
            return ComponentRef.of(p.getParameterizedType(), getQualifier(p));
        }

        private static Annotation getQualifier(AnnotatedElement element) {
            List<Annotation> qualifiers = stream(element.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
            if (qualifiers.size() > 1) {
                throw new IllegalComponentException();
            }
            return qualifiers.stream().findFirst().orElse(null);
        }
    }

    private static Injectable<Constructor<?>> getInjectConstructor(Class<?> componentType) {
        List<Constructor<?>> injectedConstructors = injectable(componentType.getConstructors()).toList();
        if (injectedConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return Injectable.of(injectedConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(componentType)));
    }

    private List<Injectable<Method>> getInjectMethods(Class<T> componentType) {
        List<Method> injectMethods = traverse(componentType, (methods, current) -> injectable(current.getDeclaredMethods())
            .filter(m -> isOverrideByInjectMethod(methods, m))
            .filter(m -> isOverrideByNoInjectMethod(componentType, m)).toList());
        Collections.reverse(injectMethods);
        return injectMethods.stream().map(Injectable::of).toList();
    }

    private List<Injectable<Field>> getInjectFields(Class<T> componentType) {
        List<Field> injectFields = traverse(componentType, (injectFields1, current) -> injectable(current.getDeclaredFields()).toList());
        return injectFields.stream().map(Injectable::of).toList();
    }

    private static <Type, Implementation extends Type> Constructor<Implementation> defaultConstructor(Class<Implementation> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
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

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] fields) {
        return stream(fields).filter(it -> it.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverrideByNoInjectMethod(Class<?> componentType, Method m) {
        return stream(componentType.getDeclaredMethods()).filter(it -> !it.isAnnotationPresent(Inject.class)).noneMatch(it -> isOverride(m, it));
    }

    private static boolean isOverrideByInjectMethod(List<Method> result, Method m) {
        return result.stream().noneMatch(it -> isOverride(m, it));
    }

    private static boolean isOverride(Method m, Method it) {
        return it.getName().equals(m.getName()) && Arrays.equals(it.getParameterTypes(), m.getParameterTypes());
    }

}

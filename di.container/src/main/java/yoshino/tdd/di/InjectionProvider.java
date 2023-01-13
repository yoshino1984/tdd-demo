package yoshino.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import yoshino.tdd.di.exception.IllegalComponentException;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
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
    private Map<Class<?>, List<Injectable<Method>>> injectMethods;
    private Map<Class<?>, List<Injectable<Field>>> injectFields;
    private Collection<Class<?>> superClasses;

    public InjectionProvider(Class<T> componentType) {
        if (Modifier.isAbstract(componentType.getModifiers())) {
            throw new IllegalComponentException();
        }

        this.injectConstructor = getInjectConstructor(componentType);
        this.superClasses = allSuperClass(componentType);
        this.injectFields = groupFields(componentType);
        this.injectMethods = groupMethods(componentType);

        if (injectFields.values().stream().flatMap(Collection::stream).map(Injectable::element).anyMatch(it -> Modifier.isFinal(it.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.values().stream().flatMap(Collection::stream).map(Injectable::element).anyMatch(it -> it.getTypeParameters().length > 0)) {
            throw new IllegalComponentException();
        }
    }

    private Map<Class<?>, List<Injectable<Field>>> groupFields(Class<?> componentType) {
        return getInjectFields(componentType).stream().collect(Collectors.groupingBy(it -> it.element().getDeclaringClass(), Collectors.toList()));
    }

    private Map<Class<?>, List<Injectable<Method>>> groupMethods(Class<?> componentType) {
        return getInjectMethods(componentType).stream().collect(Collectors.groupingBy(it -> it.element().getDeclaringClass(), Collectors.toList()));
    }

    private Collection<Class<?>> allSuperClass(Class<T> componentType) {
        List<Class<?>> superClasses = traverse(componentType, (c1, c2) -> Collections.singletonList(c2));
        Collections.reverse(superClasses);
        return superClasses;
    }

    @Override
    public T get(Context context) {
        try {
            Constructor<?> constructor = injectConstructor.element();
            constructor.setAccessible(true);
            T result = (T) constructor.newInstance(injectConstructor.toDependencies(context));

            for (Class<?> type : superClasses) {
                for (Injectable<Field> field : injectFields.getOrDefault(type, List.of())) {
                    field.element().setAccessible(true);
                    field.element().set(result, field.toDependency(context));
                }

                for (Injectable<Method> method : injectMethods.getOrDefault(type, List.of())) {
                    Method element = method.element();
                    element.setAccessible(true);
                    element.invoke(result, method.toDependencies(context));
                }
            }

            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return concat(concat(Stream.of(injectConstructor),
            injectFields.values().stream().flatMap(Collection::stream)),
            injectMethods.values().stream().flatMap(Collection::stream))
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
        List<Constructor<?>> injectedConstructors = injectable(componentType.getDeclaredConstructors()).toList();
        if (injectedConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return Injectable.of(injectedConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(componentType)));
    }

    private List<Injectable<Method>> getInjectMethods(Class<?> componentType) {
        List<Method> injectMethods = traverse(componentType, (methods, current) -> injectable(current.getDeclaredMethods())
            .filter(m -> isOverrideByInjectMethod(methods, m))
            .filter(m -> isOverrideByNoInjectMethod(componentType, m)).toList());
        Collections.reverse(injectMethods);
        return injectMethods.stream().map(Injectable::of).toList();
    }

    private List<Injectable<Field>> getInjectFields(Class<?> componentType) {
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

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] element) {
        return stream(element).filter(it -> it.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverrideByNoInjectMethod(Class<?> componentType, Method m) {
        return stream(componentType.getDeclaredMethods()).filter(it -> !it.isAnnotationPresent(Inject.class)).noneMatch(it -> isOverride(m, it));
    }

    private static boolean isOverrideByInjectMethod(List<Method> result, Method m) {
        return result.stream().noneMatch(it -> isOverride(m, it));
    }

    private static boolean isOverride(Method subMethod, Method superMethod) {
        boolean visible;
        if (subMethod.getDeclaringClass().getPackageName().equals(superMethod.getDeclaringClass().getPackageName())) {
            visible = !Modifier.isPrivate(superMethod.getModifiers()) && !Modifier.isPrivate(subMethod.getModifiers());
        } else {
            visible = (Modifier.isPublic(superMethod.getModifiers()) || Modifier.isProtected(superMethod.getModifiers()))
                && (Modifier.isPublic(subMethod.getModifiers()) || Modifier.isProtected(superMethod.getModifiers()));
        }
        return visible && superMethod.getName().equals(subMethod.getName()) && Arrays.equals(superMethod.getParameterTypes(), subMethod.getParameterTypes());
    }

}

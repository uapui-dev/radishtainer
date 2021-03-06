package net.hogedriven.backpaper0.radishtainer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Provider;
import javax.inject.Singleton;

public class Container {

    private ConcurrentMap<Descriptor, Binding> bindings = new ConcurrentHashMap<>();
    private final Scope defaultScope = (container, impl) -> {
        Object instance = container.newInstance(impl);
        container.inject(instance);
        return instance;
    };
    private final ConcurrentMap<Class<? extends Annotation>, Scope> scopes = new ConcurrentHashMap<>();
    private final Injector injector;

    public Container() {
        injector = new Injector(this);
        addScope(Singleton.class, new SingletonScope());
    }

    public void addScope(Class<? extends Annotation> annotationType, Scope scope) {
        if (annotationType == null) {
            throw new IllegalArgumentException("annotationType");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope");
        }
        Scope put = scopes.putIfAbsent(annotationType, scope);
        if (put != null && put != scope) {
            throw new RuntimeException();
        }
        Class<Scope> type = (Class<Scope>) scope.getClass();
        addInstance(type, null, scope);
    }

    public <T> void addClass(Class<T> type, Annotation qualifier, Class<? extends T> impl) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        Descriptor descriptor = new Descriptor(type, qualifier);
        impl = impl != null ? impl : type;
        check(impl);
        Scope scope = findScope(impl);
        Binding binding = Binding.newClassBinding(impl, scope);
        addBinding(descriptor, binding);
    }

    public <T> void addInstance(Class<T> type, Annotation qualifier, T instance) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (instance == null) {
            throw new IllegalArgumentException("instance");
        }
        Descriptor descriptor = new Descriptor(type, qualifier);
        Binding binding = Binding.newInstanceBinding(instance);
        addBinding(descriptor, binding);
    }

    public <T> void addProvider(Class<T> type, Annotation qualifier, Provider<T> provider) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (provider == null) {
            throw new IllegalArgumentException("instance");
        }
        Descriptor descriptor = new Descriptor(type, qualifier);
        Binding binding = Binding.newProviderBinding(provider);
        addBinding(descriptor, binding);
    }

    public <T> T getInstance(Class<T> type, Annotation qualifier) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        Descriptor descriptor = new Descriptor(type, qualifier);
        check(descriptor);
        return (T) getInstance(descriptor);
    }

    private void check(Descriptor descriptor) {
        if (bindings.containsKey(descriptor) == false && bindings.containsKey(descriptor) == false) {
            throw new NoSuchElementException();
        }
    }

    private void check(Class<?> impl) {
        if (impl.isInterface()) {
            throw new IllegalArgumentException();
        }
        if (impl.isEnum()) {
            throw new IllegalArgumentException();
        }
        ClassInfo classInfo = new ClassInfo(impl);
        List<Constructor<?>> injectableConstructors = classInfo.getInjectableConstructors();
        if (injectableConstructors.isEmpty()) {
            try {
                classInfo.getDefaultConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }
            return;
        }
        if (injectableConstructors.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (injectableConstructors.size() > 1) {
            throw new IllegalArgumentException();
        }
    }

    private Object getInstance(Descriptor descriptor) {
        Binding binding = bindings.get(descriptor);
        Object instance = binding.getInstance(this);
        return instance;
    }

    private Scope findScope(Class<?> impl) {
        return Arrays
                .stream(impl.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(
                        javax.inject.Scope.class))
                .map(a -> scopes.get(a.annotationType())).findFirst()
                .orElse(defaultScope);
    }

    public <T> Provider<T> getProvider(Class<T> type, Annotation qualifier) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        final Descriptor descriptor = new Descriptor(type, qualifier);
        check(descriptor);
        return () -> (T) getInstance(descriptor);
    }

    public Object newInstance(Class<?> clazz) {
        ClassInfo classInfo = new ClassInfo(clazz);
        List<Constructor<?>> constructors = classInfo.getInjectableConstructors();
        Constructor<?> constructor;
        if (constructors.isEmpty() == false) {
            constructor = constructors.get(0);
        } else {
            try {
                constructor = classInfo.getDefaultConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return injector.inject(constructor);
    }

    public void inject(Object target) {
        ClassInfo injectable = new ClassInfo(target.getClass());
        List<Class<?>> cs = injectable.getClasses();
        List<Field> fs = injectable.getInjectableFields();
        List<Method> ms = injectable.getInjectableMethods();
        cs.forEach(c -> {
            fs.stream().filter(f -> f.getDeclaringClass() == c)
                    .forEach(f -> injector.inject(f, target));
            ms.stream().filter(m -> m.getDeclaringClass() == c)
                    .forEach(m -> injector.inject(m, target));
        });
    }

    public void fireEvent(Object event) {
        bindings.entrySet().forEach(entry -> {
            Descriptor descriptor = entry.getKey();
            Binding binding = entry.getValue();
            fireEvent(binding, event, descriptor);
        });
    }

    private void fireEvent(Binding binding, Object event, Descriptor descriptor) {
        ClassInfo handleable = binding.getClassInfo();
        List<Class<?>> cs = handleable.getClasses();
        cs.forEach(c -> {
            handleable.getObservableMethods(event.getClass()).stream()
                    .filter(m -> m.getDeclaringClass() == c).forEach(m -> {
                        Object instance = getInstance(descriptor);
                        injector.inject(m, event, instance);
                    });
        });
    }

    private void addBinding(Descriptor descriptor, Binding binding) throws RuntimeException {
        Binding put = bindings.putIfAbsent(descriptor, binding);
        if (put != null && put != binding) {
            throw new RuntimeException();
        }
    }
}

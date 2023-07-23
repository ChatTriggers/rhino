package org.mozilla.javascript;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface JavaObjectMappingProvider {
    JavaObjectMappingProvider EMPTY = new Default();

    void findExtraMethods(Class<?> clazz, Map<MethodSignature, RenameableMethod> map, boolean includeProtected, boolean includePrivate);

    void findExtraFields(Class<?> clazz, List<RenameableField> list, boolean includeProtected, boolean includePrivate);

    class Default implements JavaObjectMappingProvider {
        @Override
        public void findExtraMethods(Class<?> clazz, Map<MethodSignature, RenameableMethod> map, boolean includeProtected, boolean includePrivate) {
        }

        @Override
        public void findExtraFields(Class<?> clazz, List<RenameableField> list, boolean includeProtected, boolean includePrivate) {
        }
    }

    final class MethodSignature {
        private final String name;
        private final Class<?>[] args;

        public MethodSignature(String name, Class<?>[] args) {
            this.name = name;
            this.args = args;
        }

        public MethodSignature(Method method) {
            this(method.getName(), method.getParameterTypes());
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MethodSignature) {
                MethodSignature ms = (MethodSignature) o;
                return ms.name.equals(name) && Arrays.equals(args, ms.args);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ args.length;
        }
    }

    final class RenameableMethod {
        private final Method method;
        private final String name;

        public RenameableMethod(Method method, String name) {
            this.method = method;
            this.name = name;
        }

        public RenameableMethod(Method method) {
            this(method, method.getName());
        }

        public Method getMethod() {
            return method;
        }

        public String getName() {
            return name;
        }
    }

    final class RenameableField {
        private final Field field;
        private final String name;

        public RenameableField(Field field, String name) {
            this.field = field;
            this.name = name;
        }

        public RenameableField(Field field) {
            this(field, field.getName());
        }

        public Field getField() {
            return field;
        }

        public String getName() {
            return name;
        }
    }
}

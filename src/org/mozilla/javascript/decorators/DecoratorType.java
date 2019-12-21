package org.mozilla.javascript.decorators;

import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Scriptable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public enum DecoratorType {
    WRAP(WrapDecorator.class, true, 1),
    REGISTER(RegisterDecorator.class, false, 2),
    USER_DEFINED(null, true, -1);

    private Class<? extends Decorator> decoratorClass;
    private boolean applyBeforeInit;
    private int argCount;

    DecoratorType(Class<? extends Decorator> decoratorClass, boolean applyBeforeInit, int argCount) {
        this.decoratorClass = decoratorClass;
        this.applyBeforeInit = applyBeforeInit;
        this.argCount = argCount;
    }

    public Class<? extends Decorator> getDecoratorClass() {
        return decoratorClass;
    }

    public static DecoratorType fromDecorator(String decorator) {
        decorator = decorator.replaceAll("@", "");

        for (DecoratorType type : values()) {
            if (type.name().toLowerCase().equals(decorator)) {
                return type;
            }
        }

        return USER_DEFINED;
    }

    public static void init(Scriptable scope) {
        try {
            for (DecoratorType type : values()) {
                Class<? extends Decorator> decoratorClass = type.getDecoratorClass();
                if (decoratorClass == null) continue;

                Method init = decoratorClass.getMethod("init", Scriptable.class);
                init.invoke(null, scope);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw Kit.codeBug();
        }
    }

    public boolean isApplyBeforeInit() {
        return applyBeforeInit;
    }

    public int getArgCount() {
        return argCount;
    }
}

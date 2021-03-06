package org.mozilla.javascript.decorators;

import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Scriptable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public enum DecoratorType {
    WRAP(WrapDecorator.class),
    REGISTER(RegisterDecorator.class),
    INITIALIZE(InitializeDecorator.class),
    NUMERICTEMPLATE(NumericTemplateDecorator.class),
    USER_DEFINED(null);

    private Class<? extends Decorator> decoratorClass;

    DecoratorType(Class<? extends Decorator> decoratorClass) {
        this.decoratorClass = decoratorClass;
    }

    public Class<? extends Decorator> getDecoratorClass() {
        return decoratorClass;
    }

    public boolean shouldTrigger(DecoratorType type) {
        return type == USER_DEFINED || this == type;
    }

    public static DecoratorType fromDecorator(String decorator) {
        decorator = decorator.replaceAll("@", "");

        for (DecoratorType type : values()) {
            if (type.name().toLowerCase().equals(decorator.toLowerCase())) {
                return type;
            }
        }

        return USER_DEFINED;
    }

    public static void init(Scriptable scope) {
        WrapDecorator.init(scope);
        RegisterDecorator.init(scope);
        InitializeDecorator.init(scope);
        NumericTemplateDecorator.init(scope);
    }
}

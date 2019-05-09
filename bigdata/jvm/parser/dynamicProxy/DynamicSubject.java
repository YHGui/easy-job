package com.katsura.jvm.parser.dynamicProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Katsura on 2017/4/21.
 */
public class DynamicSubject implements InvocationHandler{
    private Object obj;

    public DynamicSubject() {
    }
    public DynamicSubject(Object obj) {
        this.obj = obj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("Before calling " + method);
        method.invoke(obj, args);
        System.out.println("After calling " + method);
        return null;
    }
}

package com.katsura.jvm.parser.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Katsura on 2017/4/21.
 */
public class ProxyHandler implements InvocationHandler {
    private Object tar;

    //绑定委托对象，并返回代理类
    public Object bind(Object tar) {
        this.tar = tar;
//        绑定该类实现的所有接口，取得代理类
         return Proxy.newProxyInstance(tar.getClass().getClassLoader(),
                                        tar.getClass().getInterfaces(),
                                        this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
//        这里就可以进行所谓AOP编程
//        在调用具体函数方法前，执行功能处理
        result = method.invoke(tar, args);
//        在调用具体函数方法后，执行功能处理
        return result;
    }
}

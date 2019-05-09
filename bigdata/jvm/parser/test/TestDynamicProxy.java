package com.katsura.jvm.parser.test;

/**
 * Created by Katsura on 2017/4/21.
 */
public class TestDynamicProxy {
    public static void main(String[] args) {
        ProxyHandler proxy = new ProxyHandler();
        //绑定该类实现的所有接口
        Subject sub = (Subject) proxy.bind(new RealSubject());
        sub.doSomething();
        System.getProperties().put("com.misc.ProxyGenerator.saveGeneratedFiles", true);
    }
}

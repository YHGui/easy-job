package com.katsura.jvm.parser.test;

/**
 * Created by Katsura on 2017/4/21.
 */
public class RealSubject implements Subject{
    @Override
    public void doSomething() {
        System.out.println("call doSomething()");
    }
}

package com.katsura.jvm.parser.dynamicProxy;

/**
 * Created by Katsura on 2017/4/21.
 */
public class RealSubject implements Subject{
    @Override
    public void request() {
        System.out.println("From real subject");
    }
}

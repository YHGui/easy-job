package com.katsura.jvm.parser.test;

/**
 * Created by Katsura on 2017/4/21.
 */
public class SubjectProxy implements Subject{
    Subject subImpl = new RealSubject();

    /**
     * 抽象角色：声明真实对象和代理对象的共同接口，这样可以在任何可以使用目标对象的地方都可以使用代理对象。
     * 代理角色：代理对象内部含有目标对象的引用，从而可以在任何时候操作目标对象；代理对象提供一个与目标对象相同的接口，
     * 以便可以在任何时候替代目标对象。代理对象通常在客户端调用传递给目标对象之前或者之后，执行某个操作，
     * 而不是单纯地将调用传递给目标对象，同时，代理对象可以执行真实对象操作时，附加其他操作，相当于对真实对象进行封装。
     * 真实角色：定义了代理对象所代表的目标对象，代理角色所代表的真实对象，是我们最终要引用的对象，定义了代理对象所代表的目标对象。
     */
    @Override
    public void doSomething() {
        subImpl.doSomething();
    }
}

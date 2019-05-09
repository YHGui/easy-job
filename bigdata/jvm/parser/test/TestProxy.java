package com.katsura.jvm.parser.test;

/**
 * Created by Katsura on 2017/4/21.
 */

/**
 * 初看，SubjectProxy定义纯属多余，直接实例化实现类完成操作即可，但是随着业务庞大，实现proxy类对真实类的封装
 * 对于粒度的控制有着重要的意义。但是如果类方法数量越来越多，代理类的代码十分庞大。因此引入动态代理
 */
public class TestProxy {
    public static void main(String[] args) {
        Subject sub = new SubjectProxy();
        sub.doSomething();
    }
}

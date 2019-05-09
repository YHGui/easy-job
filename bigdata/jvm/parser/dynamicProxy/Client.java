package com.katsura.jvm.parser.dynamicProxy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Katsura on 2017/4/21.
 */
//客户端：生成代理实例，并调用request()方法
public class Client {
    public static void main(String[] args) {
        Subject rs = new RealSubject();//被代理类
        InvocationHandler ds = new DynamicSubject(rs);
        Class<?> cls = rs.getClass();
        Subject subject = (Subject) Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), ds);
        System.out.println(subject instanceof Proxy);//subject是Proxy的一个实例，这个实例实现了Subject接口。
        //subject的Class类是$Proxy0，这个$Proxy0类继承了Proxy，实现了Subject接口
        System.out.println("subject的Class类是： " + subject.getClass().toString());
        System.out.print("subject的属性有：");
        Field[] fields = subject.getClass().getDeclaredFields();
        for (Field field : fields) {
            System.out.print(field.getName() + ", ");
        }
        System.out.print("\n" + "subject中的方法有：");
        Method[] methods = subject.getClass().getMethods();
        //动态代理类不仅代理了显示定义的接口中的方法，而且还代理了Object类中继承而来的
        //equals()、hashcode()、toString()方法，仅此三个方法
        for (Method method : methods) {
            System.out.print(method.getName() + ", ");
        }
        System.out.print("\n" + "subject的父类是：" + subject.getClass().getSuperclass());
        System.out.print("\n" + "subject实现的接口是：");
        Class<?>[] interfaces = subject.getClass().getInterfaces();
        for (Class<?> i : interfaces) {
            System.out.print(i.getName() + ", ");
        }
        System.out.println("\n\n" + "运行结果为：");
        //调用了$Proxy0的request()方法，进而调用父类Proxy中的h的invoke()方法，
        //即InvocationHandler.invoke()方法
        subject.request();
    }
}

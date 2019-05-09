package com.katsura.jvm.parser;

/**
 * Created by Katsura on 2017/4/21.
 */
public class Autobox {
    public static void main(String[] args) {
//        当 “==”运算符的两个操作数都是 包装器类型的引用，则是比较指向的是否是同一个对象，
//        而如果其中有一个操作数是表达式（即包含算术运算）则比较的是数值（即会触发自动拆箱的过程）
        Integer a = 1;
        Integer b = 2;
        Integer c = 3;
        Integer d = 3;
        Integer e = 321;
        Integer f = 321;
        Long g = 3L;
        System.out.println(c == d);//T
        System.out.println(e == f);
        //F Integer.valueOf()实现：IntegerCache -128~127之间的数据，返回指向IntegerCache.cache中已经存在对象的引用，
        //否则创建一个新的Integer对象
//        注意，Integer、Short、Byte、Character、Long这几个类的valueOf方法的实现是类似的。
//        Double、Float的valueOf方法的实现是类似的。
        System.out.println(c == (a + b));//T
        System.out.println(c.equals(a + b));//T
        System.out.println(g == (a + b));//T
//        对于包装器类型，equals方法并不会进行类型转换
        System.out.println(g.equals(a + b));//F
    }
}

# Java 8系列之重新认识HashMap

### 摘要

HashMap是Java程序员使用频率最高的用于映射(键值对)处理的数据类型.随着JDK版本的更新,JDK1.8对HashMap底层的实现进行了优化,例如引入红黑树的数据结构和扩容的优化等.本文结合JDK1.7和JDK1.8的区别,深入探讨HashMap的结构实现和功能原理.

### 简介

Java为数据结构中的映射定义了一个接口java.util.Map,此接口主要有四个常用的实现类,分别是HashMap,HashTable,LinkedHashMap和TreeMap,类继承关系如下图所示:

![Map类继承关系图](../images/Map类继承关系.png)

下面针对各个实现类的特点做一些说明:

1. HashMap:它根据键的hashcode值存储数据,大多数情况下可以直接定位到它的值,因为具有很快的访问速度,但遍历顺序却是不确定的.HashMap最多只允许一条记录的键为null,允许多条记录的值为null.HashMap非线程安全,即任一时刻可以有多个线程同时写HashMap,可能会导致数据的不一致.如果需要满足线程安全,可以用Collections的synchronizedMap方法使HashMap具有线程安全的能力,或者使用ConcurrentHashMap.
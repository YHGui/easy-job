# web-crawler

## 流程

- web pages -> information collection -> database -> information retrieval  -> rank search  recommend
- 爬虫：scheduler-task

## 生产者-消费者模型

## 条件变量

- 在多线程程序中用来实现“等待” -> 唤醒逻辑的常用方法，实例就是：应用程序A中包含两个线程t1和t2，t1需要在bool变量test_cond为true时才能继续执行，而test_cond的值是由t2来改变的，这种情况如何解决，写程序？
- t1在test_cond为false时调用cond_wait进行等待，t2在改变test_cond的值后，调用cond_signal，唤醒等待中的t1，告诉t1，test_cond的值变了，那么t1就可以继续往下执行。

## 信号量




## 知识准备

### SQL on Hadoop

#### 调优策略

- 调优：在资源不变的前提下，让作业的执行性能有所提升．主要关注两大负载：CPU负载和IO负载(内存)

##### 架构层调优

- 分表
  - 场景：行式存储，每分钟２亿条数据，几百列数据，500个作业访问这个大表
  - 解决方法：剥离出业务表中一部分数据进行分表，仅仅关注我们目前关注的数据．
- 分区表
  - 场景：日志表(用户日志)
  - 解决办法：分区表，按时间分区，包括单级分区，多级分区和静态分区，动态分区
- 充分利用中间结果集
  - 多次使用中间结果，建表存储中间结果
    - 优点：IO负载降低
    - 缺点：增加存储
- 压缩
  - 压缩：使用压缩算法，减少数据存储空间的过程，解压缩：使用解压缩算法还原成原始数据的过程．
    - 优点：空间少，减少IO
    - 缺点：示例数据不可见(乱码)，使用前有个解压的过程
  - 使用场景：输入数据，中间数据，输出数据
  - 压缩比与解压速度：

##### 应用层调优

- 排序
  - set hive.mapred.mode 获取是否为严格模式，严格模式order by必须limit，order by只会产生一个reducer，是一个全局排序．
  - sort by 只保证每个mr作业有序，局部排序，仅仅保证分区内有序．
  - cluster by 和distribute by
    - distribute by：按照指定的字段进行数据的分发
    - cluster by = distribute by + sort by
- 控制输出(reduce/partition/task)的数量
  - 为什么要设置reduce的数量？
    - 决定文件输出的个数
    - reduce数量多，文件多，产生小文件可能性大
    - reduce数量少，运行时间长或者可能出现跑不出来情况，数据倾斜也可能出现．
    - reduce数量：min(hive.exec.reducers.max, 总输入数据量/hive.exec.reducers.bytes.per.reducer)，手动设置为：mapred.reduce.tasks，spark中对应为设置partitions数量
- 执行计划
  - explain extended "query"
  - 普通join(shuffle join)实现：
    - Mapper读取a表，Mapper读取b表
    - 数据结构：(a.id, a,...) (b.id, b,...)
    - 在shuffle阶段把相同的key分发到相同的reducer上
    - 在reducer上完成真正的join操作
  - map join(broadcast join, 并没有shuffle的过程)实现原理
    - set hive.auto.convert.join = true; 或者手动加上**/\*+mapjoin(表名)\*/**
    - 首先在本地生成一个local task,读取小表中的数据，然后将表写入hash table file，上传到HDFS缓存，然后启动一个map作业，每读取一条数据，就与缓存中的小表进行join操作，直至整个大表读取结束
    - 小表加载到缓存中
    - Mapper读取大表中的数据
    - 大表的数据和缓存中的小表数据进行对比，获取join上的结果
    - 优点：
      - 不消耗集群的reduce资源
      - 减少reduce操作，加快程序执行
      - 降低网络负载
    - 缺点
      - 占用内存
      - 生成小文件

##### 运行层调优

- 推测执行
  - 问题：集群NM/机器的负载是不一样的，集群机器配置不一样，数据倾斜(剩下一个reduce执行时间很长，任务长时间卡在99%)
  - 场景:长尾作业
  - set hive.mapred.reduce.tasks.speculative.execution;
- 并行执行
  - 前提:多个task之间没有依赖
  - set hive.exec.parallel=true, set hive.exec.parallel.thread.number=8(默认为8个)
- JVM重用
  - 背景:map和reduce task是以进程执行的,JVM数量和task数量相同,task结束JVM销毁,但是启动和销毁JVM需要资源,因此可以复用JVM来优化性能
  - set mapred.job.reuse.jvm.num.tasks=1(默认为1)

### Spark 调优
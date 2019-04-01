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

### Spark总结

#### Spark调优

##### 算子的合理选择

- map VS mapPartition      transformer
  - map是作用于RDD中的每一个元素(RDD = 100 partition partition = n个元素)
  - mapPartitions是作用于partition上,但是如果partition中元素特别多的时候,可能会出现资源不足的情况,可以调整partition数目,改变每个partition中元素的数量
- Foreach VS ForeachPartition   action
  - 类似与map VS mapPartitions,写数据库一定要使用 **Partition算子
- groupByKey VS reduceByKey
  - groupByKey shuffle传输的数据量大于reduceByKey
  - groupByKey:所有的数据都经过了shuffle
  - reduceByKey:先在本地做了聚合然后再进行shuffle(map side预聚合)
- collect() 所有数据输出到driver端内存中,因此需要数据输出较少
- coalesce和repartition对RDD分区进行重新划分,repartition为coalesce中shuffle参数为true的实现,因此若coalesce中shuffle设置为false,那么是无法将分区数由少变多的.因此coalesce适用于多分区变少,而repartition适用于少分区变多分区
  - coalesce使用于小文件合并场景,repartition适用于数据倾斜场景,也可增加或减少RDD的并行度
- cache VS persist
  - 需要经常用到(训练)的数据可以cache住
  - cacge 调用persist,persist调用默认的持久化到memory

##### 序列化的合理选择

- 使用org.apache.spark.serializer.KryoSerializer效率更高,但是需要register

##### 流处理数据Sink到目的地的N种错误操作剖析

- foreachRDD操作:

  - 数据库连接错误示例:

  - ```scala
    // 错误示例！
    dstream.foreachRDD { rdd =>
      val connection = createNewConnection()  // executed at the driver
      rdd.foreach { record =>
        connection.send(record) // executed at the worker
      }
    }
    ```

  - 这段代码的错误之处在于不理解spark的内在执行逻辑，`val connection = createNewConnection()`是在driver执行的，但是`connection.send(record)`是在executor执行的；

  - 这样就需要把driver端初始化的connection序列化之后，传到executor端使用；但问题在于connection这种对象一般是不可序列化的，所以程序会运行抛出序列化异常；

  - ```scala
    // 错误示例！
    dstream.foreachRDD { rdd =>
      rdd.foreach { record =>
        val connection = createNewConnection()
        connection.send(record)
        connection.close()
      }
    }
    ```

  - 这段代码确实是在executor端初始化的connection，但是问题在于rdd.foreach会遍历整个rdd中的数据；也就是connection的创建数量和rdd中的record数量一样，这会造成极大的资源浪费和计算延迟；

  - ```scala
    // 错误示例！
    dstream.foreachRDD { rdd =>
      rdd.foreachPartition { partitionOfRecords =>
        val connection = createNewConnection()
        partitionOfRecords.foreach(record => connection.send(record))
        connection.close()
      }
    }
    ```

  - 这段代码用rdd.foreachPartition很好的避开了上面的问题；由于foreachPartition后面传入的计算逻辑只会在每个分片上执行一次，所以connection创建的数量只会和rdd的partition数量一样多；
    但问题是spark streaming在每个mini batch都会生成一个这样的rdd，也就是说每个batch都会重复创建connection，所以资源浪费的问题依旧存在；

  - ```scala
    // 正确示例
    dstream.foreachRDD { rdd =>
      rdd.foreachPartition { partitionOfRecords =>
        // ConnectionPool is a static, lazily initialized pool of connections
        val connection = ConnectionPool.getConnection()
        partitionOfRecords.foreach(record => connection.send(record))
        ConnectionPool.returnConnection(connection)  // return to the pool for future reuse
      }
    }
    ```

  - 这段代码通过线程池实现了connection的复用，需要注意的是ConnectionPool是static的，这样就保证了每个executor只会有一个线程池存在，极大地提高了connection的复用率；

##### 如何保证流处理过程的零数据丢失

- 暂无

##### 如何基于Spark定制外部数据源

- 暂无

##### Spark常见面试题

- Spark on YARN 两种方式的区别以及工作流程
- Spark内存管理,解决OOM
- Spark作业资源设置情况 executor个数, memory, core, driver
- Shuffle机制:shuffle,依赖
- Dataframe/Dataset/RDD的区别以及编程
- 数据倾斜
- RDD
- Spark执行流程: count 后续干了什么事情
- Spark中的隐式转换的左右
- Spark和MR的区别
- Spark规模
- ThriftServer如何实现HA
- Kafka整合Spark Offset的管理
- Spark Storm Flink的区别
- 遇到问题,解决,两点
- 合理算子选择
- Catalyst的选择

### 数据倾斜

#### 数据倾斜产生的原因及现象

- 对于大数据来说,数据量大并不可怕,最怕的是数据倾斜,由于数据分布不均匀,造成数据大量集中在某个点上,造成数据热点问题,一般有shuffle,比如join或者group by
- 现象
  - 大部分task快速完成,只有极少数task执行非常慢
  - Sprak: UI对应job/stage/task
  - 例行作业运行正常,突然OOM,遇到数据倾斜需要具备自适应的能力

#### MapReduce中的shuffle

- 在shuffle的时候,必须将相同的key拉取到同一节点进行task的处理, 比如join, group by,如果某个key数量特别大,那么必然这个key对应的数据处理必然产生数据倾斜

#### Spark中的shuffle

- Spark依赖:
  - 宽依赖父RDD的partition被子RDD的某个partition使用多次,有shuffle,遇到shuffle,stage就会拆分
  - 窄依赖:父RDD的partition至多被子RDD的某个partition使用一次
- hash shuffle
- sort shuffle
- 钨丝 shuffle

#### 数据倾斜的场景

- group by
  - 实现:将groupBy字段组合为map的输出的key值,利用mapreduce的排序,在reduce阶段保存LastKey区分不同的key,最后reduce阶段聚合
- join
- count (distinct )
  - 单字段distinct实现:将groupBy字段和distinct字段组合成map的输出的key值,利用mapreduce的排序,同时将groupBy字段作为reduce的key,在reduce阶段保存LastKey即可完成去重

#### 解决办法

- join:小表和大表join,可以通过map join改善
- key分布不均匀,解决:打散key
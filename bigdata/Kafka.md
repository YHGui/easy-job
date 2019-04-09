# Kafka

## 为什么会有Kafka？

- 数据集成 Data Integration（dirty，messy）
- 上游每天产生海量日志数（访问日志，投票，评分）需要分析和处理
- 主要简单的pub／sub模型，用于一对多消费，服务解耦等场景
- 传统消息系统吞吐量低，性能差，堆积能力弱，无法承载海量数据

## 设计目标

- 分布式设计
- pub／sub模型
- 集群无限扩展，保证海量消息堆积能力
- 高可用
- 可扩展
- 高吞吐量，在高峰时段可以支持每秒数以百万计的消息传递

## 关键特色

- 高吞吐、高性能：100W+QPS
- 分布式设计&可扩展性好
  - 能够对消息进行数据分片，简称分区
  - 基于分区实现数据迁移
- 高容量
  - 使用硬盘代替内存存储
  - 多目录支持
- 读写负载均衡
  - 压力分散到集群中的每个broker上

## 关键角色

- Producer：向kafka发布消息的实例,比如日志消息生产者,用来写数据
- Consumer：从kafka订阅topic的实例,消息的消费者,用来读数据
- Broker：kafka集群中每个实例称为broker，由id唯一标示，负责消息存储转发,每一台机器叫一个Broker
  - Broker无状态:意味着消费者必须维护已消费的状态信息offset,这些信息由消费者自己维护,Broker完全不管,Broker并不知道消费者是否已经使用了该消息,Kafka创新性的解决了这个问题,将一个简单的基于时间的SLA应用于保留策略,当消息在Broker中超过一定时间后,将会自动删除
  - 消费者可以倒回到老的偏移量再次消息数据
- Topic:不同消费者去指定的Topic中读,不同的生产者往不同的Topic中写
- Partition:在Topic基础上做了进一步区分分层,由一个或多个partition组成,partition的个数决定了Topic的并发度
- Controller：每个集群中会选举一个Broker作为controller，负责执行分区，副本分配，replica leader选举，调度数据复制和迁移

## 关键术语

- Topic：kafka维护消息的种类，每一类消息由Topic标识
- Partition：对Topic中消息水平切分，至少1partition／每个topic，partition内消息有序，多个partition消息无序,每个分区代表一个并行单元
- Message:通信的基本单位,每个producer可以向一个topic发布一些消息,心发布的消息会广播给订阅了topic的consumer
- Offset:commit的log可以不断追加.消息在每个分区中都分配了一个叫offset的id序列来唯一识别分区中的消息;无论发布的消息是否被消费,Kafka都会持久化一定时间,在每个消费者都持久化这个offset在日志中,通常消费者读消息时会使offset值线性增长,但实际上其位置是由消费者控制,可以按任意顺序来消费消息
- Consumer Group：同一个Consumer Group中的Consumers，Kafka将相应的topic中的一条消息只能被一个consumer消费，用多个consumer group实现多播，一条消息被多个consumer grouop消费。一个partition只能被一个同组的consumer消费，同组的consumer则起到了负载均衡的作用，当消费者数量多于partition的数量时，多余的消费者空闲。一个consumer能消费多个partition，如果启动多个组，则会使同一个消息被消费多次,避免多个消费者消费相同的分区时会导致的额外开销(需要协调那个消费者消费哪个消息,还有锁及状态的开销)
  - Kafka中zookeeper的作用:
    - 探测broker和consumer的添加或移除
    - 当上述情况发生时,触发消费者进程的重新负载
    - 维护消费关系和追踪消费者在分区消费的消息的offset
- Replica：将partition复制，每一份叫做一个Replica。
- Replica leader：每一个Partition都有一个leader负责partition上所有的读写操作。
- Replica follower：每一个Partition都有0个或多个follower，只负责同步leader的数据
- LogEndOffset：表示每个Replica的log最后一条Message，有可能为脏数据(在page cache中)  
- Isr:全称In-Sync Replicas，是Replicas的一个子集，由leader维护isr 列表，Follower从Leader同步数据有些延迟(包括了延迟时间和延迟条数两个维度)，任意一个超过阈值都会把该Follower踢出Isr。
- Osr:全称OutOf-Sync Replicas，新Follower或从Isr列表中剔除放 Osr 列表中
- Replicas:Isr + Osr
- minIsr:如果isr.size 于minIsr.size写入不可用，目的是保证replicas数据一致性，牺牲可用性。
- highWatermark:每个replica都有highWatermark，leader和follower各自负责更新自己的highWatermark状态。

## bittiger

### big data pipeline

- 好处
  - 服务解耦
  - 高性能
  - 扩展性增强（生产者-消费者，可以单独提升某一模块的能力）
  - 解决数据冗余
  - 流量暴涨
  - queue作为缓冲带，即使consumer挂掉，再重启之后还能重新消费
  - rabbitmq需要记录消费者消费到哪一条消息了，扩展不易
- 消息分发模式
  - pull
    - 客户端定期查找，简化服务端逻辑
    - reply feature，重复消费消息
  - push
    - 服务端push消息，但是服务端需要记录push记录，更复杂
    - 但是吞吐量较高
- topic
  - 逻辑上的邮箱
  - partition（分区）
    - 并行收取消息
    - 队列变短，速度变快
    - 写到哪个partitin？hash等
- offset
  - array index
  - 分区中消息的位置，因此consumer可以定位消息，重新消费。
  - 在0.8.2之前，offset是存在zookeeper中，之后将offset存在compact topic中，offset结构：consumer group topic partition组合得到的。
- log file format
  - 每个partition就是一个文件夹
  - 消息
    - offset
    - length
    - magic value 
    - crc value 验证码
    - 真正消息的数据
  - 首先有一个segment list，包含所有数据处的位置，类似于索引，然后对应的才是真实的log数据
  - io 优化
    - 持久化	
      - Kafka存储布局简单:Topic的每个Partition对应一个逻辑日志(一个日志为相同大小的一组分段文件)
      - 每次生产者发布消息到一个分区,broker就将消息追加到最后一个段文件中.当发布的消息数量达到设定值或者经过一定的时间后,一段文件真正flush到磁盘中
      - 与传统消息系统不同,Kafka系统中存储的消息没有明确的消息Id
      - 消息通过日志中的逻辑偏移量来公开
    - append only writing
      - 无缓存设计,依赖于底层的文件系统页缓存,消息只缓存一份在页缓存中;Kafka根本不缓存消息在进程中,gc开销很小
      - disk写的时候，顺序写优化力度很大
    - zero copy
      - OS reads data from disk into pagecache in kernel space
      - Application reads data from kernel space into user space
      - Application writes data back to kernel space into socket buffer
      - OS copies data from socket buffer to NIC buffer
      - zerocopy copies data into pagecache only once and reuse
- data replication
  - Producer write through Partition Leader
  - Partition Leader write the message into local disk
  - Partition Follower pull from Partition Leader
  - Once Leader received ACK from partitions，it‘s written.
  - Kafka将日志复制到指定的多个服务器上,副本的基本单元是partition,正常情况下,每个分区有一个leader和0至多个follower
  - leader处理对应分区上所有的读写请求,分区可以多于broker数.分区可以多于broker数,leader也是分布式的
  - follower的日志和leader的日志是相同的,follower被动的复制leader,如果leader挂了,其中一个follower会自动变成新的leader
- 交付保证
  - at lease once:默认是这个策略,consumer处理顺序:获得消息--处理消息--保存位置,一旦消费的client挂掉,新的client接管时处理前面客户端已处理过的消息
  - at most once:消息可能会丢,不会重复传输
  - exactly once:每条消息肯定会被传输一次且仅传输一次
    - 实现:WAL保证,预写日志

- 问题集锦：
  - 假设topic只有一个partition，对应只有一个consumer，如果新来一个consumer，不定义consumer group的话，他就被assign到default group中，新来的consumer会怎样做：分配关系会报错，最好显示分配。
  - kafka rabbitmq redis zeromq对比：kafka性能高，而rabbitmq适用于各种不同的协议，延展性比较好，rabbitmq用erlang，维护较难，而kafka用scala，kafka只是做数据传输，kafka信息由zookeeper管理，延展性较好。

### blog

- 一个典型的kafka集群包括若干Producer，若干broker，若干consumer group，以及一个zookeeper集群。其中producer负责收集log信息，发送给broker，broker将log发送到相关的consumers。
- push or pull：
  - 在producer和broker之间，可以选择push或者pull两种模式。如果broker每次都从producer pull数据的话，往往需要在producer的本地维持一个较大的log缓存，保存broker pull之前需要存储的数据，这会增加很多复杂度。
  - 如果是从producer push的话，broker的设计会相对简单。因此Kafka使用了push模式。
  - push也会引入阻塞的问题。如果producer用的流量太多，会阻碍其他producer的数据传输。因此在最新版的Kafka中引入了限流。
  - 监控：使用30个buckets，每个bucket记录1秒的流量
  - 限制。当broker监控到一个producer超过限值的时候，延迟response消息的发送时间来减缓producer的流量
  - broker和consumer之间是consumer去pull，rabbitmq是push。
- broker里存了很多message，consumer用offset就可以知道读到哪个消息了。Kafka最早的做法是将所有consumer的offset存在zookeeper里。此时zookeeper类似于一个分布式的database。每个用户读之前，可以从database里找到对应的读取位置。这样即使consumer失败了，offset数据也不会丢失。当consumer很多的时候，zookeeper会成为性能瓶颈。因此最新版的Kafka中会专门将offset存储于一个特定的compacted topic里面。这个topic有50个partition，每个partition的Leader Broker会做为一些consumer group的coordinator。因此这些coordinator会取代zookeeper来协调consumer group。
- zero copy
  - 传统的方法是将数据首先拷贝到kernel的空间，然后拷贝到用户空间，之后再拷贝回kernel空间，最后再送到网卡。在这个过程中需要四步拷贝，浪费了大量的时间和CPU资源。因此Kafka往往借用OS提供的Zero Copy来一步将数据从硬盘拷贝到网卡。
- 如果producer个数很多，可以使用多个broker，producer可以向多个broker里写数据
  - round robin：每一次把来自用户的请求轮流分配给内部中的服务器，从1开始，直到N（内部服务器个数），然后重新开始循环。
  - 利用key的hash来写入对应的broker
  - 一个topic也可以分配为多个不同的partition，在partition内部的消息是有序的，但是在partition之间的消息是无序的。当然我们也可以通过在message中加入timestamp的方式来维持消息的序列。
  - 在Kafka中，topic指的是消息的一个集合。我们通过在发送消息和接收消息的时候指定topic来让每个consumer只是接收和处理属于自己的消息子集。
- 为了防止broker崩溃时引起的消息丢失，我们引入了primary（P）和slave（S）的概念。。P收到的每一条message都会同步给所有的S。因此在Kafka中，Primary被称为Leader，Slave被称为Follower。为了提高性能，我们可能会动态添加新的broker。这是还需要手动的启动Partition Reassignment来迁移数据。
- 如果之前的Leader失败，在剩下的followers中，谁的数据最全，就选这个broker做为新的P。
- 如果想统计在Kafka中，过去一分钟内收到了多少message。可以启动一个consumer接受所有的消息，并且每分钟将统计结果重新放回到Kafka的broker中。另外再启动一个consumer收集这些统计信息，从而给出报表。
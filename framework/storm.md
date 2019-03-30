Apache Storm is a free and open source distributed realtime computation system.

Storm makes it easy to reliably process unbounded streams of data, doing for realtime processing what Hadoop did for batch processing. Storm is simple, can be used with any programming language, and is a lot of fun to use!

Storm has many use cases: realtime analytics, online machine learning, continuous computation, distributed RPC, ETL, and more.

特点：

- 快 Storm is fast: a benchmark clocked it at over **a million tuples processed per second per node**.
- 可扩展scalable，添加机器水平扩展
- 容错fault-tolerant
- guarantees your data will be processed
- is easy to set up and operate.

Storm能实现高频数据和大规模的实时处理

## 发展历史

- Storm产生于Twitter公司
- 需求：大数据实时处理。假如自己来实现实时系统，要考虑的因素：
  1. 健壮性
  2. 扩展性/分布式
  3. 如何使得数据不丢失，不重复
  4. 高性能、低延时
- Storm VS Hadoop
  - 数据源/处理领域
  - 处理过程
    - Hadoop：Map Reduce
    - Storm：Spout Bolt
  - 进程是否结束
  - 处理速度：Storm必须快
  - 适用场景：实时和离线的区别
- Storm VS Spark Streaming
  - Storm相比Spark Streaming来说才是真正的流式处理
  - 如果能够忍受延时，且大数据处理之后需要进行机器学习等后续计算，用Spark Streaming更为合适。
- Storm优势
  - 编程模型
  - 扩展性/分布式
  - 可靠性
  - 容错性
  - 多语言
- Storm应用现状与发展趋势
  - 依赖于社区的发展、活跃度
  - 企业的需求
  - 大数据相关大会，Storm主题的数量上升
  - 互联网需求 ali  JStorm
- Storm应用案例
  - Strom在电商行业的应用
  - Storm在电信行业的应用

## Storm核心概念

- 初识Storm核心概念
  - Topology：计算拓扑，由spout和bolt组成的
  - Stream：消息流，抽象概念，没有边界的tuple构成
  - Tuple：消息/数据 传递的基本单元
  - Spout：消息流的源头，Topology的生产者
  - Bolt：消息处理单元，可以做过滤、聚合、查询/写数据库的操作。
- ISpout
  - 核心接口，负责将数据发送到topology中去处理，Storm会跟踪Spout发出去的tuple的DAG，通过ack/fail实现成功和失败机制，ack/fail/nextTuple等是在同一个线程中之心，所以不用考虑线程安全问题。
  - 核心的方法包括如下：
    - open：初始化操作
    - close：资源释放
    - nextTuple：发送数据
    - ack：tuple处理成功，storm会反馈给spout一饿成功消息
    - fail：tuple处理失败，storm会反馈发生一个消息给spout，处理失败。
  - ISpout
  - BaseRichSpout
  - IComponent
- IBolt接口：
  - 职责：接收tuple处理，并进行相应的处理(filter/join/...)
  - hold住tuple再处理，IBolt会在一个运行的机器上创建，使用Java序列化它，然后提交到主节点(nimbus)上去执行，nimbus会启动worker来反序列化，调用prepare方法，然后才开始处理tuples数据。
  - 核心方法：
    - prepare：
    - execute：
    - cleanup：
  - 实现类
- 求和案例
  - 需求：1+2+3+4+…=???
  - 实现方案：
    - Spout发送数据作为input
    - 使用Bolt来处理业务逻辑：求和
    - 将结果输出到控制台
  - 拓扑设计：DataSourceSpout —> SumBolt
- 词频统计
  - 需求：读取指定目录的数据，并实现单词计数
  - 实现方案：
    - Spout来读取指定目录的数据，作为后续Bolt处理的input，使用一个Bolt把input的数据切割开，按照逗号分割，使用一个Bolt来进行最终的单词的次数统计操作并输出。
  - 拓扑设计：DataSourceSpout ===> SplitBolt ==> CountBolt
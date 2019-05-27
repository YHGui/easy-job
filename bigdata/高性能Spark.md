# 高性能Spark

## Chapter 1 Introduction to High Performance Spark

### What is Spark and Why performance matters

### What you can expect to get from this book

### Spark Version

### Why Scala

### To be a Spark expert you have to learn a little scala anyway

### The Spark scala API is easier to use than Java API

### Scala is more performant than Python

### Why not Scala

### Learning Scala

## How Spark Works

本章主要介绍Spark并行计算模型，Spark调度器和执行引擎

### How Spark Fits into the Big Data Ecosystem

Apache Spark是一个能够提供一般化并行处理数据方法的开源框架，Spark中同样的高级函数可以在不同结构、大小的数据上完成不同数据处理任务。就其本身而言，Spark并不提供数据存储解决方案，它仅仅在Spark应用处理期间基于Spark JVMs进行计算任务。Spark可以仅仅运行在单机环境下，只有一个JVM（也被称为本地模式）。更多情况下，Spark和分布式存储系统（比如HDFS，Cassandra，S3）进行协作，一同协作的还有cluster manager，存储Spark处理后的数据，cluster manager还负责在整个集群分发Spark分布式应用。目前包含三种cluster manager：Standalone cluster manager，Apache Mesos，Hadoop YARN，其中Standalone cluster manager包含在Spark中，使用该cluster manager需要在集群的每个节点上安装Spark。

### Spark Components

Spark支持高级查询语言处理数据。

1. Spark Core：数据处理框架，API支持Scala，Java，Python，R
2. RDD：Resilient Distributed DataSets（弹性分布式数据集），lazily evaluated（懒惰计算），statically typed，distributed collections，拥有丰富的“粗粒度”transformation方法，同样拥有和分布式好存储系统进行读写操作的方法
3. Spark SQL
4. Spark MLib
5. Spark ML
6. Spark Streaming
7. GraphX


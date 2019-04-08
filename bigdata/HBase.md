## HBase

HBase是一个开源的非关系型分布式数据库,参考BigTable建模,运行于HDFS文件系统之上,可以容错存储海量稀疏的数据

### 特性与优势

- 高可靠
- 高并发读写
- 面向列
- 可升缩
- 易构建
- 海量数据存储
- 快速随机访问
- 大量写操作的应用

### 数据模型

- RowKey: Byte array,是表中每条记录的主键,方便快速查找,RowKey的设计非常重要
- Column Family:列族,拥有一个名称,包含一个或者多个相关列
- Column:属于某一个column family,family name:column name,每条记录可动态添加
- version number:类型为long,默认值是系统时间戳,可由用户自定义
- value:Byte array
- timestamp时间戳用于区分失效时间

### 物理模型

- HBase一张表由一个或多个Hregion组成,HRegion是HBase中分布式存储和负载均衡的最小单元
  - 最小单元就表示不同的HRegion可以分布在不同HRegion Server上,但一个HRegion是不会拆分到多个server上的
  - HRegion虽然分布式存储的最小单元,但并不是存储的最小单元
- 记录之间按照RowKey的字典序排列
- Region按大小分割,每个表一开始只有一个Hregion,随着数据不断插入表,Hregion不断增大,当增大到一定阈值的时候,Hregion就会等分成两个新的Hregion,当table中的行不断增多,就会有越来越多的Hregion
- 为了增加吞吐量,不同的region放在不同的机器上面
- 表->HTable
- 按RowKey范围分的region->HRegion->Region Servers
- HRegion按列族->多个HStore
- HStore->memstore+HFile(均为有序的KV)
  - memstore到一定大小会从内存flush但磁盘上面
- HFile->HDFS

### 系统架构

- Client:访问HBase的接口,并维护Cache加速Region Server的访问
- HLog:WAL预写log
- Master:负载均衡,分配Region到Region Server
- Zookeeper:保证集群中只有一个master,存储所有Region的入口(ROOT)地址,实时监控Region Server的上下线信息,并通知Master

### HBase的容错

- Zookeeper协调集群所有节点的共享信息,在HMaster和HRegion Server连接到Zookeeper后创建Ephemeral(临时)节点,并使用Heartbeat机制维持这个节点的存活状态,如果某个Ephemeral节点失效,则HMaster会收到通知,并做相应的处理
- Master容错:Zookeeper会重新选择一个新的Master
  - 无Master过程中,数据读取仍照常进行
  - 无Master过程中,region切分,负载均衡等无法进行
- Region Server容错
  - 定时向Zookeeper汇报心跳,如果一段时间内未出现心跳,Master将该RegionServer上的Region重新分配到其他Region Server上,失效服务器上"预写"日志由主服务器进行分割并派送给新的RegionServer
- Zookeeper容错
  - Zookeeper是一个可靠的服务,一般配置3或5个Zookeeper实例
- WAL(Write-Ahead-Log)预写日志
  - 是HBase的RegionServer在处理数据插入和删除的过程中用来记录操作内容的一种日志
  - 在每次put或者Delete等一条记录时,首先将数据写入到RegionServer对应的HLog文件的过程
  - 客户端往RegionServer端提交数据的时候,会写WAL日志,只有当WAL日志写成功以后,客户端才会被告诉提交数据成功,如果写WAL失败会告知客户端提交失败
  - 数据落地的过程:
    - 在一个RegionServer上的所有的Region都共享一个HLog,一次数据的提交先写WAL,写入成功后,再写memstore,当memstore到达一定阈值,就会形成一个个StoreFile(HFile)

### HBase查询过程

- 从Zookeeper中获取meta信息,包含HRegion Server信息,并缓存该位置信息,
- 从HRegion Server中查询用户Table对应请求的RowKey所在的HRegion Server,并缓存该位置信息
- 从查询到HRegionServer中读取Row
- HBase的读取流程：
  - 扫描顺序：BlockCache(region中blockcache)，Memstore，StoreFile

### HBase寻址

- 上述查询过程中第二步只缓存当前RowKey对应的HRegion的位置，因而如果下一个RowKey不在同一个HRegion中，则需要继续查询meta，随着时间推移client缓存的位置信息会越来越多，从而不需要去meta中寻找信息，除非某个HRegion宕机或者发生split情况
- meta表存储了所有用户HRegion的位置信息
  - table，key，region  -> region server

### HBase 写入

- client发起一个put请求，首先从meta表中查出该Put数据最终需要取得HRegion server，然后client将put请求发给相应的HRegion server，在HRegion server中它首先会将该Put操作写入WAL日志(Flush到磁盘)
- Memstore是一个写缓存，每一个column family有一个自己的Memstore
- Memstore是一个HRegion的一个Column Family对应的一个实例，排列顺序为：RowKey，Column Family，Column的顺序以及Timestamp的倒序

### HBase merge

- 随着写入不断增多，flush次数不断增多，Hfile文件越来越多，需要进行合并
- Compaction会从一个region的一个store中选择一些Hfile文件进行合并，原理就是从这些待合并的数据文件中读出KeyValues，再按照由小到大排列皇后写入一个新的文件中，之后，这些新生成的文件就会取代之前待合并的所有文件对外提供服务
- Minor Compaction：选取一些小的，相邻的store file将他们合并成一个更大的store file，在这个过程中不会处理已经delete或者expire的cell，一次minor compaction的结果是更小并且更大的store file
- Major Compaction：是指将所有的store file合并成一个store file，这个过程还会清理三类无意义数据：被删除的数据，TTL过期数据，版本号超过设定版本号的数据，耗时较长，占用系统资源较多
- 本质：使用段时间的IO消耗以及带宽消耗换取后续查询的低延迟

### HBase 热点问题

- RowKey设计(根据业务场景的where条件组合成RowKey)
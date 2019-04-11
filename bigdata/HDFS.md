## HDFS

分布式文件存储系统,采用master/slave结构

### NameNode

管理整个分布式系统的元数据

1. 目录树结构
2. 文件到数据库Block的映射关系
3. Block副本及其存储位置等管理数据
4. DataNode的状态监控,两者通过时间间隔的心跳来传递管理信息和数据信息,通过这种方式传递,NameNode可以获知每个DataNode保存的Block信息,DataNode的健康状况,命令DataNode启动停止等(如果发现某个DataNode节点故障,NameNode会将其负责的block在其他DataNode上进行备份)

这些数据保存在内存中,同时在磁盘保存两个元数据管理文件:fsimage和editlog

- fsimage:是内存命名空间元数据在外存的镜像文件
- editlog:各种元数据操作的WAL(write-ahead-log)文件,在体现到内存数据变化前首先会将操作记入editlog,以防止数据丢失

上述两个文件结合可以构造完整的内存数据

### Secondary NameNode

Secondary NameNode并不是NameNode的热备机,而是定期从NameNode拉取fsimage和editlog文件,并对这两个文件进行合并,形成新的fsimage文件并传回NameNode,这样做的目的是减轻NameNode的工作压力,本质上SNN是一个提供检查点功能服务的服务点

### DataNode

负责数据块的实际存储和读写工作,Block默认为64M(2.0变了128M),当客户端上传一个大文件时,HDFS会自动将其切割成固定大小的block,为了保证数据可用性,每个Block会议多备份的形式存储,默认为3.

### 文件写入过程

1. Client调用DistributeFileSystem对象的create方法,创建一个文件输出流(FSDataOutputStream)对象;
2. 通过DistributeFileSystem对象与集群的NameNode进行一次RPC远程调用,在HDFS的Namespace中创建一个文件条目(Entry),此时该条目没有任何的Block,NameNode会返回该数据每个块需要拷贝的DataNode地址信息;
3. 通过FSDataOutputStream对象,开始向DataNode写入数据,数据首先被写入FSDataOutputStream对象内部的数据队列中,数据队列由DataStreamer使用,它通过选择合适的DataNode列表来存储副本,从而要求NameNode分配新的block;
4. DataStreamer将数据包以流式传输的方式传输到分配的第一个DataNode中,该数据将数据包存储到第一个DataNode中并将其转发到第二个DataNode中,接着第二个DataNode节点会将数据包转发到第三个DataNode节点;
5. DataNode确认数据传输完成,最后一个DataNode通知clietn数据写入成功;
6. 完成想文件写入数据,Clietn在文件输出流(FSDataOutputStream)对象上调用close方法,完成文件写入;
7. 调用DistributeFileSystem对象的complete方法,通知NameNode文件写入成功,NameNode会将相关结果记录到editlog.

### 文件读取过程

1. Client通过DistributedFileSystem对象与集群的NameNode进行一次RPC调用,获取文件block信息;
2. NameNode返回存储的每个块的DataNode列表;
3. Client将连接到列表中最近的DataNode;
4. Client开始从DataNode并行读取数据;
5. 一旦Client获得了所有必须的block,它就会将这些block组合起来形成一个文件.

在处理client的读取请求时,HDFS会利用机架感知选举最接近Client位置的副本,这将会减少读取延迟和带宽消耗

### HDFS 2.0的HA实现

HDFS 1.0的架构问题:

1. 有单点问题,如果挂掉则不可用 => HA
2. 水平扩展问题 =>NameNode Federation

HA实现组件:

1. Active NameNode和Standby NameNode:两台NameNode形成互备,只有主NameNode才能对外提供读写服务;
2. ZKFailoverController(主备切换控制器,FC):作为独立进程运行,对NameNode的主备切换进行总体控制,它能及时检测到NameNode的健康状况,在主NameNode故障时借助Zookeeper实现自动的主备选举和切换
3. Zookeeper集群:为主备切换控制器提供主备选举支持
4. 共享存储系统:共享存储系统是实现NameNode的高可用最为关键的部分,共享存储系统保存了NameNode在运行过程中所产生的HDFS的元数据.主NameNode和备NameNode通过共享存储系统实现元数据同步.在进行主备切换的时候,新的主NameNode在确认元数据完全同步之后才能继续对外提供服务.
5. DataNode节点:因为NameNode和备NameNode需要共享HDFS的数据块与DataNode之间的映射关系,为了使故障切换能够快速进行,DataNode会同时向主NameNode和备NameNode上报数据块的位置信息.

### FailoverController

实现SNN和ANN之间的故障自动切换,FC是独立于NN之外的故障切换控制器,ZKFC作为NameNode机器上一个独立的进程启动,它启动的时候会创建HealthMonitor和ActiveStandbyElector这两个主要的内部组件,其中:

1. HealthMonitor：主要负责检测 NameNode 的健康状态，如果检测到 NameNode 的状态发生变化，会回调 ZKFailoverController 的相应方法进行自动的主备选举；
2. ActiveStandbyElector：主要负责完成自动的主备选举，内部封装了 Zookeeper 的处理逻辑，一旦 Zookeeper 主备选举完成，会回调 ZKFailoverController 的相应方法来进行 NameNode 的主备状态切换。

### 自动触发主备选举

NameNode在选举成功之后,会在zk上创建一个`/hadoop-ha/${dfs.nameservices}/ActiveStandbyElectorLock`临时节点,而没有选举成功的备NameNode会监控这个节点,通过Watcher来监听这个节点的状态变化事件,ZKFC的ActiveStandbyElector主要关注这个节点的NodeDeleted事件

如果Active NameNode对应的HealthMonitor检测到NameNode的状态异常时,ZKFailoverController会主动删除当前在Zookeeper上建立的临时节点`/hadoop-ha/${dfs.nameservices}/ActiveStandbyElectorLock`


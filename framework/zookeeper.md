# zookeeper

## 应用场景(use case)

- 计算问题：
  - 分布式计算，单机计算能力提升有限，同时价格昂贵，因此考虑到分发给多个机器计算。
  - 分布式系统问题：机器之间通过网络通信，难以调度，通信成本增大。
  - master-slave model（hdfs hbase kafka等应用此模式，Cassandra使用peer to peer模式）
    - master：掌握任务列表，分发任务
    - slave：完成任务，汇报master任务完成情况和自身状态
    - issues：
      1. master如何选取？
      2. master挂掉如何解决？
      3. slave worker挂掉如何解决？group membership
      4. master和slave不能通信如何解决？metadata management
- zookeeper（chubby）
  - 本身也是一个高性能吞吐的小集群cluster，负责维护和管理分布式集群。 
  - 强一致性，消息具有顺序性，保证持久化
  - 为其他节点在并发情况下做同步协调
  - 处理并发更简单
- zookeeper 架构
  - 协调方式：
    - 发送消息：
      1. 网络不可靠，延迟
      2. 处理速度不一致
      3. 机器时钟不一致
      4. master，backup master， many workers，部分slave workers不能和master联系，而是和backup master联系。
    - shard storage
      1. 更为直接
      2. 仍然依赖于网络
      3. zookeeper采用这种协调方式
- 内部实现：分层结构，类似于文件系统。
  - 文件系统，类似于树 data tree
    - 根目录下有一个server，保护serverId，相当于master，其他的slave可以去查看master
    - znode：类似于一个文件夹，基本单元
      - persistent znode：除非显示删除，否则持久存在
      - ephemeral znode：临时znode，如果客户端断开链接，则znode将被删除
      - sequential znode：序列化数字，会持续上升。
    - watcher
      - 帮助客户端了解znode的状态变化，set watcher之后，如果parent znode下的children znode有变化之后，其他机器得到反馈就会执行相应的local动作
      - 如果znode状态改变，将通知客户端
      - 通知是一次性的操作，如果需要继续观察，需要再生成一个watcher
      - 避免轮询（polling）的race conditions（竞态条件）
    - client-server interaction
      - tcp连接
      - 客户端只连接最新状态的服务器（zookeeper）沟通
      - 创建session timeout
      - 读可以从任何一台服务器读取
      - 写操作需要写到最新的zookeeper服务器
    - zxid：64位integer id，高32位表示当前leader是谁，低32位为自动递增的标识符，znode更新后会产生新的zxid
    - leader 选取
      - zookeeper的角色：leader，follower（跟随者），observer
      - 状态：looking，leading，following
    - state replication 
      - zab协议
        - leader
          - ping request ack revalidate
          - 收到写请求
          - 将请求转化为一个transaction
          - leader发proposal消息给所有的follower
          - 一旦大部分follower接收请求，leader开始发送commit消息，服务器数量一般为奇数。
        - follower
          - 从leader接收proposal
          - 验证是否为leader（split-brain）
          - 验证transaction是否顺序正确（写入操作顺序执行）
          - 发ack给leader
          - 接收leader的commit消息
          - follower改变自身data tree
          - follower和leader之间维护session，follower会发送revalidate，保证延长session。
- zookeeper解决分布式问题
  - leader election
    - 和follower通信
    - 如果leader挂掉，那么follower顶上，开始选取，进行投票，投票是为了统计信息（机器id，zxid），选出状态最新。
  - crash detection
    - heartbeat，通过ephemeral znode实现
  - group membership
    - manage the group and create ephemeral znode under group node
  - metadata management
    - 机器（broker）配置信息存放在zookeeper上面，改也是在zookeeper上改。
    - 比如kafka，会将其topic和broker的信息存放在zookeeper上面。
    - 比如hbase，做水平扩展的时候，需要zookeeper配置机器信息
- 和eureka区别
  - zookeeper满足的是CAP（C-数据一致性，A-服务可用性，P-服务对网络分区故障的容错性）中的CP，即任何时刻对zookeeper的访问请求能得到一致的数据结果，同时系统对网络分割具备容错性，但不能保证每次服务请求的可用性（也就是说在极端环境下，zookeeper可能会丢弃一些请求，消费者呈现需要重新请求才能获得结果）。zookeeper是分布式协调服务，他的职责是保证数据在其管辖下的所有服务之间保持同步，一致。
  - 相比来说，zookeeper作为分布式协同服务非常好，但是作为服务发现不是很合适，因为服务发现就算是饭和了包含不实信息的结果也比什么都不返回要好，再者，对于服务发现而言，宁可返回某服务5分钟之前在哪几个服务器上可用的信心，也不能因为暂时的网络故障而找不到可用的服务器，而不返回任何结果。所以说用zookeeper做服务发现肯定是错误的。
  - 更何况，如果zookeeper被用作服务发现，zookeeper本身并没有正确的处理网络分割的问题，而在云端，网络分割问题跟其他类型的故障一样的确会发生。
  - “在zookeeper中，如果在同一个网络分区的节点数达不到zookeeper选取leader的法定人数，他们就会从zookeeper中断开，当然同时也不能提供服务发现了”
  - 在eureka中，如果某台服务器宕机后，不会有类似zookeeper的选leader的过程，客户端请求会自动切换到新的eureka节点，当宕机的服务器重新恢复后，eureka会再次将其纳入到服务器的集群管理中，而对于他要做的无非是同步一些新的服务注册信息而已。所以，再也不用担心掉队的服务器恢复以后，会从euraka服务器集群中剔除出去的风险。
  - eureka内置了心跳服务，用于淘汰一些濒死的服务器，如果在eureka中注册的服务，心跳变得迟缓时，eureka会将其整个剔除出管理范围，类似于zookeeper，但是当网络分割故障发生时，这是非常危险的，因为那些因为网络问题而被剔除出去的服务器本身是很健康的，只是因为网络分割故障把eureka集群分割成了独立的子网而不能互访而已。而netflix考虑解决这个问题：当eureka服务器节点在短时间里丢失了大量的心跳连接，那么这个eureka节点会进入自我保护模式，同时保留那些心跳死亡的服务注册信息不过期，此时，eureka节点对于新的服务还能提供注册服务，对于死亡的仍然保留，防止还有客户端向其发起请求，当网络故障恢复后，euraka节点会退出自我保护模式。
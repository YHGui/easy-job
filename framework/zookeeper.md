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
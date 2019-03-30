# zookeeper：分布式系统从入门到实战

## 什么是一致性

### CAP Theorem

对于一个分布式系统，不能同时满足一下三点：

- 一致性（Consistency）
  - 弱一致性
    - 最终一致性
      - DNS：将域名和ip进行绑定，需要保证各种根域名服务器一步步往上同步
      - Gossip协议，Cassandra
  - 强一致性
    - 同步
    - Paxos
    - Raft
    - ZAB
- 可用性（Availability）：分布式系统提供的服务是经常可以访问到的。
- 分区容错性（Partition Tolerance）：分布式系统在遇到某节点或网络分区故障的时候，仍然能够对外提供满足一致性和可用性的服务。
  - 多个节点（比如五个），三个在美国，两个在中国，如果两地的节点由于硬件原因出现故障，保证仍然能够对外提供满足一致性和可用性的服务。

## 明确问题

- 数据安全
  - 数据不能存在单点上，容错。
  - 分布式系统解决方案：state machine replication的共识（consensus）算法
  - paxos其实是个共识算法，系统的最终一致性，不仅需要达成共识，还会取决于client的行为。

## 强一致性算法

- 主从异步复制

  - 比如Mysql的binlog复制，步骤如下：
    1. 主接到写请求
    2. 主写入本磁盘
    3. 主应答OK
    4. 主复制数据到从库
    5. 问题存在于如果在复制前，磁盘损坏了，那么数据将丢失

- 主从同步复制

  1. 主接到写请求
  2. 主复制日志到从库
  3. 从库这时可能阻塞
  4. 客户端一直在等待应答“OK”，直到所有从库返回
  5. 问题在于一个节点导致整个系统不可用，没有数据丢失，可用性降低

- 主从半同步复制

  1. 主接到写请求
  2. 主复制日志到从库
  3. 从库这时可能阻塞
  4. 如果1<=x<=n个从库返回“OK”，则返回客户端“OK”
  5. 高可靠性，高可用性，问题是可能出现任何从库都不完整，因此需要多数派写。

- 多数派写

  - 每次写入都保证写入N／2个节点，每次读都保证从大于N／2个节点中读。
  - 问题是在处理并发问题的情况无法保证系统正确性，顺序非常重要。

- Paxos

  - Lamport发明：Paxos的希腊城邦
  - 执行条件：没有数据丢失和错误，容忍：消息丢失，消息乱序
  - Basic Paxos
    - Client：系统外部角色，请求发起者。类比民众。
    - Proposer：接受client请求，向集群提出提议propose。并在冲突发生时，起到冲突调节的作用，类比议员，人大代表，替民众提出议案，发起Paxos的进程。
    - Acceptor：提议投票和接收者，只有在形成法定人数（Quorum，一般为多数派）时，提议才会最终被接受，类比国会，国内人大，存储节点，接受、处理和存储消息。
    - Learner：提议接收者，backup，备份，对集群一致性没什么影响。类比记录员。
    - 步骤、阶段
      1. Phase 1a：Prepare
         - proposer提出一个提案，编号为N，此N大于这个proposer之前提出提案编号。请求acceptors的Quorum接受
      2. Phase 1b：Promise
         - 如果N大于acceptor之前接受的任何提案编号则接受，否则拒绝。
      3. Phase 2a：Accept
         - 如果达到了多数派，是否达到多数派是由proposer来决定的，proposer会发出accept请求，此请求包含提案编号N，以及提案内容。
      4. Phase 2b：Accepted
         - 如果此acceptor在此期间没有收到任何编号大于N的提案，则接受此提案内容，否则忽略。
    - client发出request，proposer就提出一个提案，prepare(1)，然后acceptors来根据多数派接受这个提案，promise(1, {va, vb, vc})，如果达到多数派，proposer会发出accept请求，请求包括提案编号以及提案内容，如果此acceptor在此期间没有收到任何编号大于N的提案，则接受此提案内容，否则忽略。
    - 除proposer外的其他accpetor节点失败，基本上不会影响，只要保证quorum即可，如果proposer节点失败，则propose无效，重新选取leader proposer进行propose，之前的操作没有完成，没有被accept。
    - 潜在问题：活锁，两个proposer循环向集群acceptor提出提议。解决办法：类似网络中解决冲突的办法：随机等待时间消除冲突。
    - 难实现，效率低（2轮RPC），活锁
    - 这里可以将client看成是客户端，proposer是一个服务请求的转发者，acceptor是数据节点，proposer收到客户端写数据的日志请求之后，首先开始对多个acceptor发起这个请求的讨论，然后经过多数派同意，且讨论的请求编号没有超过当前请求编号的，接受，proposer经过判断，满足多数派，发出请求的提案编号以及提案内容，如果acceptor在此期间还没收到编号更大的提案，就接受此提案内容，然后会发送给learner，learner也会接受。
  - Multi Paxos
    - Leader：唯一的proposer，所有的请求都经过这个leader proposer。
      - 首先第一轮RPC是竞选总统的过程，就是prepare(n)，然后所有的acceptor表示同意是第I任总统，promis(n, I, {va, vb, vc})，总统选好了之后只有一个proposer
    - 一轮RPC：accept(N, I, Vm)和accepted(N, I, Vm)这个阶段就够了
    - 下一次又来一个请求，并不需要再竞选总统，还处在任期之内，后续只有一轮RPC
    - 减少角色，进一步简化，省略proposer角色，从三个server中选择master，一轮竞选之后，选出master，然后开始另外一轮RPC的过程，更像master-slave模型。
  - Fast Paxos
    - 没有冲突，一轮RPC确定一个值
    - 有冲突，2轮RPC确定一个值

- Raft

  - 简化版的Paxos
  - 划分为三个子问题
    - leader election：一个server发送请求给其他的server来表明自己想竞选为leader，然后得到同意，leader会心跳给其他follower，同时心跳包中夹杂了请求数据。
    - log replication：竞选出leader之后，leader接收到client请求，转化为事务，发出proposal给follower，然后follower接受并验证之后会写入log，并会返回ack，表明已经写入了，然后leader自己commit，然后告诉follower写入，commit。
    - safety
  - 重定义角色（server）
    - leader
    - follower
    - candidate（leader失败了，竞选的临时状态，竞选者）

  ​
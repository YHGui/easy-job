# 背景

熟悉分布式系统的开发者都知道“一致性算法”（Consensus Algorithm）的重要性，它是构建正确一致分布式系统的关键点，也是难点。

著名的一致性算法有Paxos，当然该算法也是出名的难以理解和实现。而Raft算法是当前公认的容易理解且得到大量实践验证的一致性算法，虽然从2013年发表至今不过两三年，但是已经被广为接受，并有了大量的应用和开源实现。

要弄懂Raft算法，直接读英文论文（见参考1）当然是最好的，但需要花一些时间。目前网上关于Raft算法的中文介绍也有不少，有一些也很不错（见参考2），但大部分只是简单介绍，对算法的整体脉络把握和正确性证明都不尽如意。

所以经过多次重读论文，我写了这篇文章，希望能帮助读者更好地理解Raft算法。该文章会一步步介绍算法的各个关键点，然后给出严格的正确性证明，最后给出一个简洁的“32235总结”。

# 关键字

- Replicated State Machine
- Strong Leader
- Term
- Leader/Follower/Candidate
- Leader Election
  - 心跳（Heartbeat）
  - 选举超时（Election Timeout）
  - 多数投票（Majority Vote）
  - 投票分歧（Split Vote）
  - 随机退让（Random Election Timeout）
  - 投票限制（Election Restriction）
  - 强制复制（Force Duplication）
- Log Replication
  - 多数复制（Majority Replicated）
  - 日志提交（Log Committed）
  - 一致性检查（Consistency Check）
  - 提交限制（Committing Restriction）

# 问题描述

一致性算法通常都基于“**复制状态机**”（Replicated State Machine）模型：

*在一个分布式数据库系统中，如果各节点的初始状态一致，且每个节点都执行相同且确定性的（deterministic）的操作序列，那么它们最后都能得到一致的状态。*

通过“复制状态机”模型，一组节点可以对外表现为一个高可用高一致的状态机，并能容忍一定程度的节点失败或者消息丢失等问题。

![复制状态机](http://loopjump.com/wp-content/uploads/2016/10/state_machine.png)

一致性算法的核心工作就是为了**保证每个节点都执行相同的操作序列**。图中的“Consensus Module”就是实现一致性算法的模块。

![Consensus Module](https://img-blog.csdn.net/20140804203840619?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvY3N6aG91d2Vp/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

在实际系统中，操作序列通常以Log形式存储在节点上。“Consensus Module”在节点间通信，使用一致性算法保证每个节点都拥有完全一致的Log（Replicated Log），即相同的操作序列。这些操作序列按照严格的顺序执行到状态机上，就能得到一致的状态机。

# 算法描述

Raft算法最重要的考虑就是**“可理解性”（Understandability）**，以此作为算法设计和策略选择的最高准则。这一点正是针对Paxos算法的“难以理解性”。

在作者看来，Raft算法具有如下优势：

- 算法简单易懂
- 解释完整详细
- 有开源实现和实际应用验证
- 有规范化的正确性证明
- 性能较好

为了提高可理解性，作者在设计算法时应用了一些思想和技术：

- 分治思想：将复杂的一致性问题分解为Leader选举、Log复制、Safety保证、Membership改变等子问题
- 简化思想：通过减少不确定性，从而减小状态空间，降低问题复杂度。Raft算法在设计过程中增加一些限制条件来简化问题，包括：
  - 采用Strong Leader结构，Client只与Leader交互
  - Log不允许有空洞
  - Leader只允许追加Log Entry，不允许删除和移动Log Entry
  - Log数据只能从Leader向非Leader单向流动
  - 在Log Replication过程中加入了一致性检查（Consistency Check）的限制
  - 在Leader Election过程中加入了投票限制（Election Restriction）以保证Leader的日志完整性（Leader Completeness）
  - 在新Leader向Follower同步日志的过程中加入了提交限制（Committing Restriction）

## Strong Leader

在设计分布式系统时，对于节点拓扑结构通常有两种选择：

- **Symmetric, leader-less**
  所有Server都是对等的，Client可以和任意Server进行交互。
- **Asymmetric, leader-based**Server间不是对等的，在任意时刻有且仅有一台Server拥有决策权，称为Leader，Client仅和该Leader交互。

为了降低复杂度和提高可理解性，Raft选择了后者，论文中称之为“**Strong Leader**”，其Strong体现在：

- Leader全权负责与Client的交互
- Leader全权负责分配写请求的Log序号，无需与非Leader协商
- Log数据只允许从Leader向非Leader单向流动

## Term

Raft将时间划分为一个一个的“**时间片**”（Term），每个时间片的长度可以是任意的。

Term可以认为是一种“逻辑时间”， 其作用是帮助识别“过期信息”（Obsolete Information），以避免分布式系统中的“时钟同步”难题。在论文第2节提到，分布式系统的一致性最好不依赖于服务器的时钟同步。

​     ![Term](https://img-blog.csdn.net/20140804203911429?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvY3N6aG91d2Vp/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

Term具有如下特性：

- Term总是单调递增的
- 正常情况下，一个Term会涵盖两个阶段：选举阶段和正常阶段
- 在一个Term的选举阶段，每个Server只允许为一个Candidate投票，投票遵循“先到先得”（first-come-first-served）原则
- 每个Term中最多只允许存在一个Leader，但可以允许不存在Leader，譬如遇到投票分歧（Split Vote）的情况
- 每个Server都会在本地维护一个currentTerm
- 并不是每个Server都会看到所有Term，也就是说，Server的currentTerm一定是单调递增的，但不一定是连续的
- Server之间会通过RPC交换Term，如果发现别的Server拥有更高的Term，就将自己的currentTerm更新为高值

## Roles

Raft将Server分为三种角色，对应三种状态：

- **Leader**
  全权负责与Client的交互，任意时刻系统中最多只能存在一个Leader。
- **Follower**
  总是被动响应RPC请求，从不主动发起RPC请求。
- **Candidate**
  由Follower向Leader转换的中间状态，该状态下会发起选举投票，选举成功就会成为Leader。

## Heartbeat

Leader会定期向各个Follower发送“**心跳**”（Heartheat），以维持自己的Leader权威，防止Follower发起选举投票。

Follower会维护一个“**选举超时器**”（Election Timeout），每次收到心跳都会重置超时器。如果一段时间都没有收到任何心跳，选举超时器就会超时，从而触发选举投票。

## RPC

Server之间通过RPC传递消息，主要有两种RPC消息：

- **RequestVote**
  由Candidate发起，用于在选举过程中向其他Server收集投票。
- **AppendEntries**
  由Leader发起，用于向Follower复制Log和维持Heartbeat。在不携带Log数据时，就是单纯的Heartbeat。

RPC在实现时通常有如下特性：

- 可重试：当RPC发送失败或者超时，会不断进行重试
- 可并行：当向多个Server发送RPC时，可以并行发送，以提高效率

## Role Switching

Server会在三种状态间切换，状态转换图如下：

​        ![状态切换](https://img-blog.csdn.net/20140804203847296?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvY3N6aG91d2Vp/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

总的来说：

- Server初始化时总是先进入Follower状态
- Follower和Candidate这两种状态可以在不同条件下相互转换或者保持原态
- Follower必须经过Candidate中间状态才能成为Leader，Leader可以降级为Follower

细化来说，各状态转换路径的触发条件为：

- **Follower => Follower**

  ：Follower在收到合法RPC后会重置选举超时器，维持Follower状态不变。合法RPC包括：

  - AppendEntries RPC with newTerm >= currentTerm：表示Leader维持不变或者选举出了新的Leader
  - RequestVote RPC with newTerm > currentTerm：表示发起了新一轮选举投票

- **Follower => Candidate**：Follower的选举超时器超时，转入Candidate状态，并开始发起选举投票

- **Candidate => Follower**

  ：Candidate在选举过程中可能会收到来自其它Server的合法RPC，从而中断自己的选举过程，转为Follower。合法RPC包括：

  - AppendEntries RPC with newTerm >= currentTerm：发现有其他Server已经成为了Leader，就会“归附”
  - RequestVote RPC with newTerm > currentTerm：发现有其他Candidate也发起了选举投票且拥有更高的Term，就会主动“禅让”

- **Candidate => Candidate**：Candidate的选举超时器超时，发起新一轮选举投票

- **Candidate => Leader**：Candidate在选举过程中获得超过半数Server的投票，就会“上台”（step up ）成为Leader

- **Leader => Follower**：Leader发现别的Leader或者Candidate拥有更高的Term，就会主动“下台”（step down）

## Leader Election

Candidate发起选举的过程：启动选举超时器，自增currentTerm，首先投票给自己，然后并行向其他Server发起RequestVote RPC，如果RPC发送失败则不断重试，直到遇到以下任意一种情况：

- 获得超过半数Server（包括自己）的投票（Majority Vote），转换为Leader，并开始广播Heartbeat
- 接收到合法Leader（newTerm >= currentTerm）的AppendEntries RPC，转换为Follower
- 接收到合法Candidate（newTerm > currentTerm）的RequestVote RPC，转换为Follower
- 选举超时器超时，发起新一轮选举投票

在选举过程中，造成选举超时器超时的一种常见情况是“**投票分歧**”（Split Vote）：

- 在某一段时间，多个Server都进入Candidate状态，都先给自己投票，然后向其他Server发送RequestVote RPC收集投票
- 由于各Server可能会投向不同的Candidate，投票出现分歧，可能造成没有任何Candidate能够收到超过半数的投票，选举失败
- 投票分歧只能通过发起新一轮选举投票来解决，但是新的投票过程又可能出现投票分歧，甚至一直循环下去
- Raft的解决办法是使用“随机退让”策略，即每个Candidate的选举超时使用一个范围内的随机值，譬如150~300ms之间

Raft的选举投票是如何保证一个Term中最多只允许存在一个Leader的？

- 在一个Term的选举阶段，每个Server只允许为一个Candidate投票
- Candidate只有在选举中获得超过半数Server的投票，才能成为Leader

## Log Replication

Leader全权负责与Client的交互，将Client发送的操作命令通过Log的形式复制到所有Server上，这个过程就是“**日志复制**”（Log Replication）。

正常情况下，Log Replication的流程为：

- Client发送操作命令给Leader

- Leader将命令作为一条Log Entry追加到本地Log，每条Log Entry都会记录当前的currentTerm（Log Term）和在Log中的位置（Log Index）

- Leader向所有的Follower发送携带该Log Entry的AppendEntries RPC

- Follower收到RPC后，将Log Entry追加到本地Log，向Leader响应复制成功

- 当Leader确认超过半数的Server（包括自己）都复制成功（Majority Replicated）后，就认为该Log是“

  已提交的

  ”（committed），接下来：

  - 更新自己记录的最高committed位置（commitIndex）
  - 将该命令执行（apply）到状态机，完成后该Log被认为是“**已执行的**”（applied），于是更新自己记录的最高applied位置（lastApplied），并向Client返回执行结果
  - Leader通过后续的AppendEntries RPC将自己最新的commitIndex传递给Follower
  - Follower收到新的commitIndex后，将位于commitIndex的命令（连同在其之前还未执行的命令）按照顺序执行到本地状态机

​    ![Log Replication](https://img-blog.csdn.net/20140804203703250?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvY3N6aG91d2Vp/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

## Consistency Check

为了保证日志的一致性（见后面Safety中的Log Matching），Raft算法对Log Replication过程进行了强化，加入了“**一致性检查**”（Consistency Check）：

- Leader在AppendEntries RPC中携带prevLogTerm和prevLogIndex，即当前要发送的Log Entry之前那条Entry的Term和Index

- Follower收到RPC后，首先会

  检查prevLog是否一致

  （位于prevLogIndex位置的Log Entry存在，且其Term与prevLogTerm一致）：

  - 如果prevLog一致，则检查发送过来的Log Entry在本地Log中是否已经存在：
    - 如果不存在，则直接append到本地，然后向Leader回复成功
    - 如果存在且Term一致，则直接向Leader回复成功（RPC幂等性）
    - 如果存在且Term不一致（出现冲突），则用发送过来的Log Entry覆盖本地的Entry，并删除掉后面的所有Entry，然后向Leader回复成功
  - 如果prevLog不一致，则直接向Leader回复失败

在正常情况下，Consistency Check都不应当失败。但是如果Leader宕机了，就可能出现日志缺失、增多、不一致等情况；而如果Leader多次宕机的话，情况就会更复杂。譬如：

​        ![Consistency Check](http://static.zybuluo.com/zhenlanghuo/ti6rfwc59rj5grivps5igycv/1752522-fc1352afc54b5ce7.png)

当Log出现不一致时，Leader有责任将不一致的Follower同步得与自己一致，做法就是“**将自己的Log强制复制到Follower**”（Force Duplication）。这是“Leader具有最高权威”的一个体现，也是“Log数据只允许从Leader向非Leader单向流动”的关键所在。

为此，Leader在内存中专门为每个Follower维护了两组变量：

- matchIndex：记录该Follower与Leader日志一致的最高位置。初始化为0，因为不知道一致到什么位置了。
- nextIndex：记录Leader向该Follower发送的下一条AppendEntries RPC的日志位置。初始化为Leader当前最新Log的下一个位置，因为总是乐观地认为一致到最新位置了。

日志同步过程分为两步：

- 找同步点（find match point）：如果Leader向Follower发送AppendEntries RPC返回失败，说明日志出现了不一致，Leader将该Follower对应的nextIndex减1，然后重发AppendEntries RPC。如果还是失败，则接着减1再重发，一直重复该过程直到Follower返回成功，此时Follower与Leader的日志达成一致，或者说找到了match point，于是Leader更新matchIndex。而Follower则会将同步点之后的冲突数据都删除掉。
- 复制日志（replicate log）：在找到同步点后，Leader就按照正常的Log Replication流程依次将后面的Log Entry都复制给Follower。

针对每次减1可能太慢的问题，Raft中也提到了一些优化，譬如跳跃减。

## Election Restriction

为了保证Leader日志的完整性（见后面Safety中的Leader Completeness），Raft算法对Leader Election过程也进行了强化，加入了“**投票限制**”（Election Restriction）：

- Candidate在RequestVote RPC中携带lastLogTerm和lastLogIndex，即当前最后一条Log Entry的Term和Index
- 其他Server在收到RPC后，会检查自己的lastLog是否比Candidate的“更新”（more up-to-date），如果是，则不给Candidate投票

其中“**up-to-date**”的定义为：

- 首先比较lastLogTerm，Term大的一方被认为是“more update-to-date”
- 在lastLogTerm相等的情况下，再比较lastLogIndex，Index大的一方被认为是“more update-to-date”

形式化表示就是：

"A is more update-to-date than B"  **<=>**  ( lastLogTermA > lastLogTermB || ( lastLogTermA == lastLogTermB && lastLogIndexA > lastLogIndexB ) )

Raft的完整性检查是如何保证每个Leader的Log一定都包含当前为止所有committed项的？直观的解释就是：

- 一条Log Entry如果是committed，那么一定被复制到了超过半数的Server上
- 一个Candidate如果成为了Leader，那么一定得到了超过半数Server的投票，假设为其投票的Server集合为Majority（Candidate也属于Majority）
- 那么至少有一个Server既复制了Log Entry又进行了投票，即对于任意一条Committed Log Entry，在Majority中至少存在一个Server，其Log中包含该Log Entry
- 由于Candidate获得了Majority中所有Server的投票，那么Candidate的Log一定是Majority中“至少最新的”（at least as up-to-date as any other log），那么Candidate的Log一定包含该Log Entry

之所以只是直观解释（严格证明请参见后面Safety中的Leader Completeness），是因为上面的推导还有漏洞：

- 虽然一条Log Entry在committed的时候被复制到了超过半数的Server上，但是Server上的Log在后面还可能被覆盖或者删除掉
- 即使Candidate的Log一定是“most up-to-date”的，那么Candidate的Log就一定包含所有Committed Log Entry吗？

对于“Server上的Log可能会被覆盖或删除掉”的漏洞，可以构想如下场景：

（图中每个方框就是一个Log Entry，方框中的数字是Log Term，最上面一排数字是Log Index）

​    ![]()

- (a) S1是Leader，当前正在复制Log[2]（Log at Index 2）
- (b) S1宕机，S5被选为新的Leader（得到S3和S4的投票），并接收到Client的新命令，append到本地Log
- (c) S5宕机，S1重启并被选为新的Leader（得到S2和S3的投票），继续复制Log[2]到S3，此时Log[2]在3台机器上完成了复制，满足了“多数复制”条件
- (d) S1宕机，S1重启并被选为新的Leader（得到S3和S4的投票），复制自己的Log[2]到其他机器，覆盖了其他机器上原来的Log[2]

在以上场景中，(c)中黄色方框的Log[2]满足了“多数复制”条件，如果按照原来的定义，它应当是committed，但是后来却被覆盖了。

如果一条Committed Log Entry后来在某个Server上被覆盖或删除掉了，就不能保证超过半数的Server上一定有该日志，也就没法保证日志的完整性。

## Committing Restriction

为了修复上述漏洞，Raft引入了一个新的限制，我觉得可以称之为“**提交限制**”（Committing Restriction）：

- Leader永远不会因为达到了“多数复制”的条件就提交以前Term（previous term）的数据
- 只有当前Term（current term）的数据才会因为达到了“多数复制”的条件而被提交
- 以前Term的数据被提交只有一种办法：被位于其后面的当前Term的数据连带着提交（因为如果一个Log Entry被提交，那么其之前的数据也会被一并提交）

通俗解释就是：如果一个Server刚成为Leader，它会从本地状态机中获得lastApplied位置，并将commitIndex也初始化为lastApplied；然后它可能会发现Log尾部有一批日志数据没有被提交，这批日志是属于以前Term的（称为老日志）；Leader会将这批老日志复制到各Follower上，但是即使这批日志满足了“多数复制”条件，也不能被提交；Leader需等待Client发来新的操作命令，新命令的日志会使用当前Term（称为新日志）；只有新日志在满足“多数复制”条件被提交时，才能连带着将老日志一起提交。

为什么新日志能因为达到“多数复制”条件被提交，而老日志不能呢？我觉得主要原因是：老日志不能保证自己是所有Server中最新的（most up-to-date），在后面有可能被覆盖或删除；但是新日志一定能保证是最新的，因为新日志用的是当前Leader的Term，根据“Term总是单调递增”和“只有Leader才能分配新日志”，可以确定其他Server上不可能存在更新（more up-to-date）的日志。

Committing Restriction的引入，保证了Committed Log Entry在任何Server上都不可能被覆盖或删除掉：

一条Log Entry是committed  **<=>**  该Log Entry被复制到了超过半数的Server上，且不可能被覆盖或删除

譬如针对前面的场景，引入Committing Restriction后：

​    ![]()

- (c) S5宕机，S1重启并被选为新的Leader（得到S2和S3的投票），继续复制Log[2]到S3，此时Log[2]在3台机器上完成了复制，虽然满足了“多数复制”条件，但是因为是老日志仍然不能提交，因为有可能在后来被覆盖或删除，譬如走路径(d)
- (e) S1继续复制Log[3]到S2和S3，此时Log[3]在3台机器上完成了复制，满足了“多数复制”条件，且因为是新日志可以提交，此时commitIndex才可以更新到位置3，表示Log[2]和Log[3]都是committed；如果此时S1宕机的话，因为S4和S5的日志都不够新，根据Completeness Check它们都不可能选为新的Leader，所以能够保证S1、S2、S3上的Log[2]和Log[3]不会被覆盖或删除

# Safety证明

Raft为了证明算法正确性，引入了5个属性。Raft保证在任意时刻以下5个属性都为真：

- Election Safety
- Leader Append-Only
- Log Matching
- Leader Completeness
- State Machine Safety

其中“State Machine Safety”是最终目标，是复制状态机一致性的保证。前面4个都是为其服务的。

## Election Safety

选举安全：

*在一个Term中最多只允许存在一个Leader。*

在Leader Election中，Raft通过两方面限制来保证这一点：

- 在一个Term的选举阶段，每个Server只允许为一个Candidate投票
- Candidate只有在选举中获得超过半数Server的投票，才能成为Leader

反证法：假设某个Term中同时选举产生两个Leader-A和Leader-B，根据选举过程定义，A和B必须同时获得超过半数Server的投票，那么至少存在一个Server同时给A和B投票，与“每个Server只允许为一个Candidate投票”的限制条件矛盾。

## Leader Append-Only

Leader日志仅支持追加：

*Leader从不“重写”（overwrite）或者“删除”（delete）本地Log，只会“追加”（append）本地Log。*

这是Raft算法对Leader强加的限制。

## Log Matching

日志一致性：

*如果两个Server上有一条Log Entry具有相同的Term和Index，那么这两个Server上在[1,Index]范围内的Log Entry都一致。*

Raft分两步对其进行了证明：首先证明具有相同Term和Index的日志项相同，然后证明位于Index之前的所有日志项均相同。

- 第一步证明两个Server上的Log Entry如果具有相同的Term和Index，那么它们存储的命令也是一样的。由前面的“Election Safety”和“Leader Append-Only”可证明：
  - 一个Term最多只有一个Leader
  - Leader全权负责与Client交互，Log Entry只能由Leader创建，通过Log Replication复制到其他Server上，且复制过程保持Term和Index不变，即：Log Entry一旦被创建，位置和内容都不会再变化
  - Leader是Append-Only的，不会删除和修改已有的Log，所以Leader其Term范围内，对于同一个Index位置最多只会创建一个Entry
- 第二步证明位于Index之前的所有日志项均相同。这一点由前面的“Consistency Check”来保证，借助归纳法即可证明：
  - 最初Log为空的时候，Log Matching是满足的
  - 每次Log增长的时候，“Consistency Check”都能维持Log Matching属性

## Leader Completeness

日志完整性：

*如果一条Log Entry被某个Leader提交（committed），那么此后所有Leader的Log中都包含该Log Entry。*

反证法：假设Raft算法无法保证“Leader Completeness”属性，即存在一个“Term=T”的Leader(T)，其提交了一条“Index=X”的日志项Log(X)，但是在后续某些Leader的日志中不包含Log(X)，不妨令这些Leader中Term最小的为Leader(U)，显然“**U>T**”。

1. Leader(U)不包含Log(X)，根据“Leader Append-Only”属性，Leader(U)在发起选举的时候也不包含Log(X)；
2. 因为Leader(T)提交了Log(X)，所以Log(X)一定被复制到了超过半数的Server上；因为Leader(U)成为了Leader，所以它一定得到了超过半数Server的投票；那么至少存在一个Server既复制了Log(X)又对Leader(U)进行了投票，不妨取其中一个称之为Voter，而该Voter就是证明的关键；
3. 可以确定Voter复制Log(X)一定发生在其对Leader(U)进行投票之前，否则如果Voter先对Leader(U)进行了投票，其currentTerm也会变成U，由于“**U>T**”，此后Voter会拒绝所有Leader(T)发来的过期AppendEntries RPC，也就无法复制Log(X)；
4. 也可以确定Voter在对Leader(U)投票的时候一定包含Log(X)，因为根据假设，Leader(U)是不包含Log(X)的Term最小的Leader，那么如果在Leader(T)和Leader(U)中间还存在其他Leader，这些Leader也都包含Log(X)，由于Leader自己不会删除日志，而Follower只会删除与Leader有冲突的日志，所以可以保证Voter一直都不会删除Log(X)；
5. 由于Voter投票给了Leader(U)，那么根据“Election Restriction”，可以确定Voter在对Leader(U)投票的时候其日志不比Leader(U)的更新，根据“up-to-date”的定义，有两种情况：
   1. 两者的lastLogTerm相等：此时Leader(U)的Log长度应不小于Voter，即Leader(U)的Log集合包含Voter的Log集合，所以Leader(U)一定也包含Log(X)，与第1条矛盾；
   2. Leader(U)的lastLogTerm大于Voter：可得Leader(U)的lastLogTerm>T，因为通过Voter包含Log(X)可知Voter的lastLogTerm至少为T。令Leader(U)的lastLog为“Index=Y”的Log(Y)，lastLogTerm为W，则满足“**T<W<U**”且“**X<Y**”。根据假设可知，创建Log(Y)的Leader(W)包含Log(X)，又因为Leader(W)和Leader(U)都包含同一条日志Log(Y)，根据“Log Matching”属性，Leader(U)一定也包含Log(X)，与第1条矛盾；
6. 以上得证

## State Machine Safety

状态机安全：

*一旦某个Server将一条Log Entry执行到了状态机，那么其他Server在相同位置只会执行相同的Log Entry。*

证明：

- 一旦某个时刻某个Server将一条Log Entry执行到了状态机，那么这条Log Entry一定是committed，且一定和Leader上的一致
- 考虑最早在Index=X位置上执行Log(X)的Server，其Leader一定提交了Log(X)；根据“Leader Completeness”属性，该Leader后续所有Leader的Log中都包含Log(X)，也就是说后续所有Server在该位置上都执行相同的Log(X)

有了“State Machine Safety”保证，加上对Log的按序执行，就能保证所有的Server都执行相同的操作命令序列，从而保证一致性。

# 总结

Raft算法核心总结（32235总结）：

- 三个角色
  - Leader
  - Follower
  - Candidate
- 两个过程
  - Leader Election（Leader选举）
  - Log Replication（日志复制）
- 两个多数
  - Majority Vote（多数投票）
  - Majority Replication（多数复制）
- 三个限制
  - Election Restriction（投票限制）
  - Replication Restriction（复制限制）
  - Committing Restriction（提交限制）
- 五个保证
  - Election Safety
  - Leader Append-Only
  - Log Matching
  - Leader Completeness
  - State Machine Safety

![]()

注：本文没有讨论Membership改变的情况。

# 参考

1. Paper: *In Search of an Understandable Consensus Algorithm (Extended Version)*
2. <https://raft.github.io/>
3. [Raft一致性算法](http://blog.csdn.net/cszhouwei/article/details/38374603)
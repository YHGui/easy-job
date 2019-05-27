## YARN

### 背景和对比

#### Hadoop1.0和Hadoop2.0对比:

Hadoop1.0:

- JobTracker必须不断跟踪所有TaskTracker和所有的map，reduce任务
  1. 用户Client提交了一个Job,Job的信息会发送到JobTracker中,JobTracker是Map-Reduce框架的中心,它需要与集群中的机器通过心跳定时通信,需要管理哪些程序应该跑在哪些机器上,需要管理所有job的失败,重启等操作
  2. TaskTracker上的任务都是JobTracker来分配的,MR集群中每台机器都有的一个组件,它做的事情主要是监视组件所在机器的资源使用情况
  3. TaskTracker同时监视当前机器的tasks运行状况.TaskTracker需要把这些信息通过心跳发送给JobTracker,JobTracker会搜集这些信息以便处理新提交的job,来决定其应该分配运行在哪些机器上
- 弊端
  1. JobTracker是MR的集中处理点,存在单点故障;
  2. JobTracker赋予的功能太多,导致负载过重,未将资源管理与作业控制分开,导致负载重而且无法支撑更多的计算框架,当集群作业非常多时,会有很大内存开销;
  3. 在TaskTracker端,以map/reduce task的数目作为资源表示过于简单,没有考虑到cpu/没存的占用情况,如果两个大内存消耗的task被调度到了一个节点,很容易出现oom;
  4. 在TaskTracker端,把资源强制划分为map task slot和reduce task slot,如果当系统中只有map task或者只有reduce task的时候,会造成资源的浪费,也就是前面提过的集群利用问题.

Hadoop2.0:

JobTracker和TaskTracker被RM，AM，NM取而代之,其中JobTracker的两个主要功能:资源管理(负责整个集群的资源,包括内存,CPU,磁盘的管理)和作业控制(直接与应用程序相关的模块,且每个作业控制进程只负责管理一个作业),分拆成两个独立的进程

### YARN架构

全称:Yet Another Resource Negotiator,整体为Master/Slave架构,RM为master, NM为Slave

#### Resource Manager

RM拥有为系统中所有应用资源分配的决定权，是中心服务，做的事情就是调度，启动每一个Job所属的Application，监控Application的存在情况；与运行在每个节点的NM进程交互，通过心跳通信，达到监控NM的目的.

- RM代替集群资源管理器，主要有两个组件:
  1. 调度器:Scheduler
  2. 应用程序管理器:Applications Manager, ASM

##### 调度器

- 根据容量,队列等限制条件,将系统中的资源分配给各个正在运行的应用程序,该调度器作为一个纯调度器,不再从事任何与应用程序相关的工作,比如不负责重新启动(因应用程序失败或者硬件故障导致的失败),这些均交由应用程序相关的ApplicationMaster完成
- 处理客户端请求，接收JobSubmitter提交的作业，根据提交的参数和NM收集的状态信息，启动调度过程，分配一个Container作为AＭ,Container是一个动态资源分配单位,将内存,CPU,磁盘,网络等资源封装在一起,从而限定每个任务使用的资源量.此外,该调度器是一个可插拔的组件,用户可根据自己的需求设计新的调度器,YARN提供了多种可直接用的调度器,比如Fair Scheduler和Capacity Scheduler.

##### 应用程序管理器

应用程序管理器负责管理整个系统中所有的应用程序,包括应用程序提交,与调度器协商资源,监控AM运行状态并在失败时重新启动等.

#### NodeManager

NM是每个节点运行的资源和任务管理器,一方面,它会定时像RM汇报本节点上的资源使用情况和各个Container的运行状态;另一方面,它接收并处来来自AM的Container启动/停止等各种请求.

NM代替TaskTracker

- 总的来说，在单节点上进行资源管理和任务管理
- NM是slave进程，类似TaskTracker的角色，是每个机器框架的代理，处理来自RM的任务请求
- 接受并处理来自AM的Container的启动，停止等各种请求
- 负责启动应用程序的Container(执行应用程序的容器)，并监控他们的资源使用情况(CPU，内存，磁盘和网络)，并报告给RM

#### Application Master

提交的每个作业都会包含一个AM,主要功能包括:

1. 与RM协商资源,获取合适的Container;
2. 将得到的任务进一步分配给内部的任务;
3. 与NM通信以启动/停止任务;
4. 监控所有任务的运行状态,当任务有失败时,重新为任务申请资源并重启任务

AM代替一个专用且短暂的JobTracker(任务管理)
- 应用程序的Master，每一个应用对应一个AM，在用户提交一个应用程序时，一个AM的轻量型进程实例会启动，AM协调应用程序内的所有任务的执行
- 负责一个Job生命周期内所有工作，类似之前的JobTracker，每一个Job都有一个AM，运行在RM之外的机器上
- 与NM协同工作与Scheduler协商合适的Container进行Container的监控

- 一个分布式应用程序代替一个MapReduce作业

### YARN作业提交流程

当用户向YARN中提交一个应用程序后,YARN将分两个阶段运行该应用程序:第一个阶段是启动ApplicationMaster;第二个阶段是由ApplicationMaster创建应用程序,为它申请资源,并监控它的整个运行过程,直到运行完成

YARN工作流程步骤:

1. 用户向YARN提交应用程序,其中包括ApplicationMaster程序,启动ApplicationMaster命令,用户程序等;
2. RM为应用程序分配第一个Container,并与对应的NM通信,要求它在这个Container中启动应用程序的ApplicationMaster;
3. ApplicationMaster首先向RM注册,这样用户可以直接通过NM查看应用程序的运行状态,然后它将为各个任务申请资源,并监控它的运行状态,直到运行结束,一直重复下面的4-7步;
4. ApplicationMaster采用轮询的方式通过RPC协议向RM申请和领取资源;
5. 一旦ApplicationMaster申请到资源后,便与对应的NM通信,要求它启动任务;
6. NM为任务设置好运行环境(包括环境变量,jar包等)后,将任务启动命令写到一个脚本中,并通过运行该脚本启动任务;
7. 各个任务通过某个RPC协议向ApplicationMaster汇报自己的状态和进度,以让ApplicationMaster随时掌握各个任务的运行状态,从而可以在任务失败时重新启动任务;
8. 应用程序运行完成后,ApplicationMaster向RM注销并关闭直接.

### 调度器

YARN调度器是一个可插拔的组件,社区提供:FIFO,Capacity, Fair Scheduler,也可以自定义调度器

#### FIFO Scheduler

资源调度时,按照队列的先后顺序,先进先出的进行调度和资源分配

#### Fair Scheduler

将用户的任务分配到多个资源池中,每个资源池设定资源分配最低保障和最高上限,管理员也可以指定资源池的优先级,优先级高的资源池将会被分配更多的资源,当一个资源池有剩余时,可以临时将剩余资源共享给其他资源池,调度过程如下:

1. 根据每个资源池的最小资源保障,将系统中的部分资源分配给各个资源池;
2. 根据资源池的指定优先级将剩余资源按照比例分配给各个资源池;
3. 在各个资源池中,按照作业的优先级或者根据公平策略将资源分配给各个作业;

公平调度器有以下几个特点:

1. 支持抢占式调度,即如果某个资源池长时间未被分配到公平共享量的资源,则调度器可以杀死过多分配资源的资源池的任务,以空出资源共这个资源池使用;
2. 强调作业之间的公平性:在每个资源池中，公平调度器默认使用公平策略来实现资源分配，这种公平策略是最大最小公平算法的一种具体实现，可以尽可能保证作业间的资源分配公平性;
3. 负载均衡:公平调度器提供了一个基于任务数目的负载均衡机制，该机制尽可能将系统中的任务均匀分配到给各个节点上;
4. 调度策略配置灵活:允许管理员为每个队列单独设置调度策略;
5. 提高最小应用程序响应时间:由于采用了最大最小公平算法，小作业可以快速获得资源并运行完成;

#### Capacity Scheduler

能力调度器是 Yahool 为 Hadoop 开发的多用户调度器，应用于用户量众多的应用场景，与公平调度器相比，其更强调资源在用户之间而非作业之间的公平性。

它将用户和任务组织成多个队列，每个队列可以设定资源最低保障和使用上限，当一个队列的资源有剩余时，可以将剩余资源暂时分享给其他队列。调度器在调度时，优先将资源分配给资源使用率最低的队列（即队列已使用资源量占分配给队列的资源量比例最小的队列）；在队列内部，则按照作业优先级的先后顺序遵循 FIFO 策略进行调度。

能力调度器有以下几点特点：

1. **容量保证**：管理员可为每个队列设置资源最低保证和资源使用上限，而所有提交到该队列的应用程序共享这些资源；
2. **灵活性**：如果一个队列资源有剩余，可以暂时共享给那些需要资源的队列，而一旦该队列有新的应用程序提交，则其他队列释放的资源会归还给该队列；
3. **多重租赁**：支持多用户共享集群和多应用程序同时运行，为防止单个应用程序、用户或者队列独占集群中的资源，管理员可为之增多多重约束；
4. **安全保证**：每个队列有严格的 ACL 列表规定它访问用户，每个用户可指定哪些用户允许查看自己应用程序的运行状态或者控制应用程序；
5. **动态更新配置文件**：管理可以根据需要动态修改各种配置参数。

### YARN容错

- NM挂掉：NodeManager 如果超时，则 ResourceManager 会认为它失败，将其上的所有 container 标记为失败并通知相应的 ApplicationMaster，由 AM 决定如何处理（可以重新分配任务，可以整个作业失败，重新拉起）；
- AM挂掉：ResourceManager 会和 ApplicationMaster 保持通信，一旦发现 ApplicationMaster 失败或者超时，会为其重新分配资源并重启。重启后 ApplicationMaster 的运行状态需要自己恢复，比如 MRAppMaster 会把相关的状态记录到 HDFS 上，重启后从 HDFS 读取运行状态恢复；
- Container容错:如果 ApplicationMaster 在一定时间内未启动分配的 container，RM 会将其收回，如果 Container 运行失败，RM 会告诉对应的 AM 由其处理；
- RM挂掉：单点故障，基于zookeeper做高可用,分别有一个active master和一个standby master

#### YARN Cluster 和 YARN Client

- 本质是AM进程的区别,cluster模式下,driver运行在AM中,负责向YARN申请资源,并监督作业运行状况,当用户提交完作业后,就关掉client,作业会继续在YARN上运行,cluster不适合交互类型作业.而client模式,AM仅向YARN请求executor,client会和请求的container通信来调度任务,即client不能离开

#### Spark 和hadoop作业之间的区别

- 一个MR就是一个job,而一个job里面可以有一个或多个task,task又可以区分为Map Task和Reduce Task
  - MR问题:
    - 调度慢
    - 计算慢,中间结果落磁盘
    - API抽象简单
    - 缺乏作业流描述,一项任务需要多轮MR
- MR的每个Task分别在自己的进程中运行,当该Task运行完时,进程也就结束,也会释放掉进程占用的资源
- Spark只有当任务执行完了才会一次性把所有资源释放掉
- Spark中
  - 应用程序:由一个driver program和多个job构成
    - SparkContext(Spark应用的入口,创建需要的变量,还包含集群的配置信息等)
    - 将用户提交的job转换为DAG图(类似数据处理的流程图)
    - 根据策略将DAG图划分多个stage,根据分区从而生成一系列tasks
    - 根据tasks要求向RM申请资源
    - 提交任务并检测任务状态
  - executor:真正执行task的单元,一个worker node上可以有多个executor
  - job:由多个stage组成
  - stage:对应一个taskset
  - taskset:对应一组关联的相互之间没有shuffle依赖关系的task组成
  - task:任务最小的工作单元(每个partition的计算就是一个task,task是调度的基本单元)
  - 同样有job的概念,但是和MR不一样
  - 一个Application和一个SparkContext相关联,每个Application中可以有一个或多个job,可以并行或串行运行job
  - Spark中一个action可以触发一个job运行
  - 在job里面包含多个stage,stage以shuffle进行划分,stage中包含多个task,多个task构成了task set
  - 和MR不一样,Spark中多个task可以运行在一个进程里面,而且这个进程的生命周期和application一样,即使没有job运行
  - 优点:
    - 加快Spark运行速度,Task可以快速启动
    - 并处理内存中的数据,最大化利用内存cache,中间结果放内存,加速迭代,将结果集放内存,加速后续查询和处理,解决运行慢的问题
    - 完整作业描述
  - 缺点:每个Application拥有固定的executor和固定数目的内存
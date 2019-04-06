## YARN

### 背景和对比

- hadoop1.0和hadoop2.0对比：
  - JobTracker必须不断跟踪所有TaskTracker和所有的map，reduce任务，TaskTracker上的任务都是JobTracker来分配的
  - JobTracker和TaskTracker被RM，AM，NM取而代之
    - RM代替集群资源管理器，处理客户端请求，接收JobSubmitter提交的作业，根据提交的参数和NM收集的状态信息，启动调度过程，分配一个Container作为AＭ，RM拥有为系统中所有应用资源分配的决定权，是中心服务，做的事情就是调度，启动每一个Job所属的Application，监控Application的存在情况；与运行在每个节点的NM进程交互，通过心跳通信，达到监控NM的目的
    - AM代替一个专用且短暂的JobTracker(任务管理)
      - 应用程序的Master，每一个应用对应一个AM，在用户提交一个应用程序时，一个AM的轻量型进程实例会启动，AM协调应用程序内的所有任务的执行
      - 负责一个Job生命周期内所有工作，类似之前的JobTracker，每一个Job都有一个AM，运行在RM之外的机器上
      - 与RM协商资源
        - 与Scheduler协商合适的Container
      - 与NM协同工作与Scheduler协商合适的Container进行Container的监控
    - NM代替TaskTracker
      - 总的来说，在单节点上进行资源管理和任务管理
      - NM是slave进程，类似TaskTracker的角色，是每个机器框架的代理，处理来自RM的任务请求
      - 接受并处理来自AM的Container的启动，停止等各种请求
      - 负责启动应用程序的Container(执行应用程序的容器)，并监控他们的资源使用情况(CPU，内存，磁盘和网络)，并报告给RM
  - 将JobTracker两个主要的功能分离成单独的组件，这两个功能是资源管理和任务调度/监控
  - 一个分布式应用程序代替一个MapReduce作业

### 容错能力

- RM挂掉：单点故障，基于zookeeper做高可用
- NM挂掉：通过心跳方式通知RM，RM将情况通知对应AM
- AM挂掉：RM负责重启，AM重启无需重新运行已完成的task
- NameNode HA
  - active NameNode，standby NameNode，在任何时间只有一台机器处于active状态，另一台机器处于Standby状态，一旦active NN挂掉，Standby将顶上
  - 同步：Journal Nodes守护进程，完成元数据n/2+1的同步
  - 任何情况下，NameNode只有一个active状态，否则导致数据的丢失及其它不正确的结果
    - 在任何时间JNs只允许一个NN充当writer，在故障恢复期间，将要变成active状态的NN将取得writer的角色，并组织另外一个NN继续处于active状态(FC)

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
  - executor:真正之心task的单元,一个work node上可以有多个executor
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
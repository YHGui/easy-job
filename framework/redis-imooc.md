# Redis 慕课网学习笔记

## Redis初识

### Redis是什么

- 开源
- 基于key-value的存储服务系统
- 多种数据结构：字符串，hash tables，list，Sets，sorted Sets，bitmap，geo
- 高性能、功能丰富

### Redis的特性回顾

- 速度快
  - 10w OPS
  - 数据存在内存中，使用c语言
  - 单线程
- 持久化
  - Redis所有数据保存在内存址，对数据的更新将异步地保存在磁盘中
- 多种数据结构
  - 多种数据结构，见上
  - BitMaps
  - HyperLogLog：超小内存唯一值计数
  - GEO：地理信息定位
- 支持多种客户端语言
- 功能丰富
  - 发布订阅
  - Lua脚本
  - 事务
  - pipeline
- 简单
  - 23000 c语言代码
  - 不依赖外部库
  - 单线程模型
- 主从复制
  - 主服务器和从服务器
- 高可用、分布式 
- Redis使用场景
  - 缓存系统
  - 计数器
  - 消息队列系统
  - 排行榜
  - 社交网络
  - 实时系统
- Redis常用配置
  - 是否配置为守护进程
  - 端口配置（默认为6379）

### Redis单机安装

## API的理解和使用

- 通用命令
  - keys: keys *, keys [pattern] 一般不在生产环境使用，scan，热备从节点，O(n)
  - dbsize：计算key的总数，可以生产使用，redis内置计数器，时间复杂度为O(1)
  - exists key：判断key是否存在，O(1)
  - del key [key]: 删除指定的key-value，O(1)
  - expire key seconds：在seconds秒后过期，O(1)
  -  ttl key ： 查看key的剩余过期时间，O(1)
  - persist key ： 去掉key的过期时间，O(1)
  - type：返回key的类型，O(1)
- 数据结构和内部编码
  - 举例：hash，内部为hashtable和ziplist
  - 字符串键值结构：
    - value可以是字符串，整型，二进制，json串等格式，本质都是二进制，大小不超过512MB，一般100K左右
    - 场景：
      - 缓存，计数器，分布式锁等等
      - 记录网站每个用户个人主页的访问量
        - incr userid : pageview，单线程：无竞争
      - 缓存视频的基本信息（数据源在MySQL中）：根据vid从server获取视频信息，首先经过redis，不存在则访问MySQL，并写入到redis
      - 分布式id生成器：多个服务，希望每次获取的id都是自增的，使用redis来实现，它是单线程的
    - 命令：
      - get set del复杂度均为O(1)
      - incr：incr key自增1，默认初始值为0，复杂度均为O(1)
      - decr：decr key自减1，默认初始值为0，复杂度均为O(1)
      - incrby：key自增k，不存在自增后为k，复杂度均为O(1)
      - decrby：key自减k，不存在自减后为-k，复杂度均为O(1)
      - set key value，不管是否存在，都设置，复杂度均为O(1)
      - setnx key value，key不存在，才设置，add操作，复杂度均为O(1)
      - set key value xx，key存在，才设置，update操作，复杂度均为O(1)
      - mget key1 key2 key3批量操作，省去大量网络时间，mget操作的个数有上限，复杂度为O(n)
      - mset key1 key2 key3批量操作，省去大量网络时间，mset操作的个数有上限，复杂度为O(n)
      - getset key newvalue：set key newvalue并返回旧的value，复杂度均为O(1)
      - append key value：将value追加到旧的value，复杂度均为O(1)
      - strlen key 返回字符串的长度，注意中文，复杂度均为O(1)
      - incrbyfloat key 3.5:增加key对应的值为3.5，复杂度均为O(1)
      - getrange key start end:获取字符串指定下标所有值，复杂度均为O(1)
      - setrange key index value：设置指定下标所有对应的值，复杂度均为O(1)
  - hash
    - 结构：
      - key : field value，可以单独更新属性，filed不能相同
    - 命令：
      - hget key field，复杂度均为O(1)
      - hset key field value，复杂度均为O(1)
      - hdel key field ，复杂度均为O(1)
      - hexists key filed：判断hash key是否有field，复杂度均为O(1)
      - hlen key：获取hash key filed的数量，复杂度均为O(1)
      - hmget：批量获取hash key的一批field对应的值，复杂度均为O(n)
      - hset：批量设置hash key的一批field value，复杂度均为O(n)
      - hgetall key：返回hash key对应所有的field和value，复杂度均为O(n)
      - hvals key：返回hash key对应所有filed的value，复杂度均为O(n)
      - hkeys key：返回hash key对应所有field，复杂度均为O(n)
      - hsetnx key field value：如果field已存在，则失败，复杂度均为O(1)
      - hincrby key field intCounter：对于value自增intCounter，复杂度均为O(1)
      - hincrbyfloat key field floatCounter：自增浮点数版，复杂度均为O(1)
  - list列表
    - 特点
      - key：value为有序队列
      - 有序，可以重复，左右两边插入弹出
    - 命令
      - 增
        - rpush key value1 value2…valuen，时间复杂度为O(1-n)
        - lpush key value1 value2…valuen，时间复杂度为O(1-n)
        - linsert key before|after value newValue在list指定的值前／后插入newValue，时间复杂度为O(n)
      - 删
        - lpop key 从左侧弹出一个item O(1)
        - rpop key 从右侧弹出一个item O(1)
        - lrem key count value：根据count的值，从列表删除所有value相等的项，count大于0，从左到右删除count个value相等的项，count小于0，从右到左删除count绝对值个value相等的项，count为0，则删除所有value相等的项，时间复杂度为O(n)
        - ltrim key start end：按照索引范围修剪列表，时间复杂度为O(n)
      - 查
        - lrange key start end：获取列表指定范围所有的item，包含end，时间复杂度为O(n)
        - lindex key index：获取列表对应下标的值
        - llen key：获取列表长度
      - 改
        - lset key index newValue：改变制定下标值
      - blpop/brpop key timeout：阻塞弹出，timeout为阻塞超时时间，时间复杂度为O(1)
    - 实战
      - timeline 
  - set集合
    - 特点
      - 集合元素无序，无重复
      - 支持集合间操作
      - 小心使用，防止大量集合元素
    - 命令
      - sadd key element：向集合key添加element，如果已经存在，则添加失败，时间复杂度为O(1)
      - srem key element：删除，时间复杂度为O(1)
      - scard：算出集合大小
      - sismember user:1:follow it = 1判断it是否在集合中
      - srandmember user:1:follow count = his 从集合中随机挑出count个元素
      - spop user:1:follow = sports 从集合中随机弹出一个元素，集合变了
      - smembers user:1:follow = music his sports it 获取集合所有元素
      - sinter：集合间交
      - sdiff：集合间差
      - sunion：集合间并
      - sdiff|sinter|sunion + store destkey
    - 实战
      - 抽奖系统：spop，sranmember
      - like，赞，踩
      - tag：给用户添加标签
      - 共同关注
    - tips
      - sadd = tagging
      - spop／srandmember = random item
      - sadd + sinter = social graph
  - 有序集合zset
    - 特点
      - 有序，无重复
    - 命令
      - zadd key score element：添加操作，时间复杂度为O(logn)，使用的跳表skiplist
      - zrem key element：删除元素，时间复杂度为O(1)
      - zscore key element：获取分数时间复杂度为O(1)
      - zincrby key increScore element：增加或减少分数，时间复杂度为O(1)
      - zcard key：返回元素的总个数，时间复杂度为O(1)
      - zrange key start end：返回指定索引范围内的升序元素分值，时间复杂度为O(logn + m)
      - zrangebyscore key minScore maxScore：返回指定分数范围内的元素，时间复杂度为O(logn + m)
      - zcount key minScore maxScore：返回有序集合内在指定分数范围内的个数，时间复杂度为O(logn + m)
      - zremrangebyrank key start end：删除指定排名内的升序元素 ，时间复杂度为O(logn + m)
      - zremrangebyscore key minScore maxScore：删除指定分数内的升序元素，时间复杂度为O(logn + m)
    - 实战
      - 排行榜
    - tips
      - zrevrank
      - zrevrange
      - zrevrangebyscore
      - zinterstore
      - zunionstore
  - redisObject：数据类型type，编码方式encoding，数据指针，虚拟内存，其他信息等
- 单线程架构
  - 串行：一次只运行一条命令，拒绝长命令，其实不是单线程
  - 单线程为何如此快？
    - 纯内存
    - 非阻塞IO：epoll，IO多路复用
    - 避免线程切换和竞态消耗

## Redis客户端的使用

- jedis
  - 基本使用
    - Maven依赖
    - Jedis对象，包含host和port，connectionTimeout连接超时，soTimeout读写超时。
  - Jedis连接
    - 直连
    - 连接池

## 瑞士军刀Redis

### 慢查询

- 生命周期
  - 发送命令—排队—执行命令—返回结果 
  - 客户端超时不一定有慢查询，但慢查询是客户端超时的一个可能因素
- 两个配置
  - slowlog-max-len
    - 先进先出队列
    - 固定长度
    - 保存在内存内
  - slowlog-log-slower-than 
    - 慢查询阈值
    - slowlog-log-slower-than  = 0，记录所有命令
    - slowlog-log-slower-than < 0，不记录任何命令
- 三个命令
  - slowlog get [n]：获取慢查询队列
  - slowlog len：获取满查询队列长度
  - slowlog reset：清空
- 运维经验
  - slowlog-max-len不要设置过大，默认为10ms，通常设置为1ms
  - slowlog-log-slower-than不要设置过小，通常设置为1000左右
  - 理解命令的生命周期
  - 定期持久化慢查询

### pipeline

- 流水线
  - 1次时间 = 一次网络时间+一次命令时间 
  - N次时间 = n次网络时间+n次命令时间
  - 1次pipeline = 1次网络时间+n次命令 
- 使用建议
  - 注意每次pipeline携带的数据量
  - pipeline只能作用载一个Redis节点上
  - M操作：原子操作，pipeline：非原子操作

### 发布订阅

- 模型
  - publisher —> server，订阅者订阅server，可以订阅多个频道
  - N次时间 = n次网络时间+n次命令时间
  - 1次pipeline = 1次网络时间+n次命令 
- API
  - publish sohu:tv "hello world"
  - subscribe sohu:tv
  - unsubscribe
  - psubscribe [pattern]：订阅模式
  - punsubscribe：退订指定的模式
  - pubsub channels：列出至少有一个订阅者的频道
  - pubsub numsub：看有多少订阅者
  - pubsub numpat：列出被订阅模式的数量
- 消息队列
  - "抢"，只有一个订阅者能抢到

### bitmap

- 流水线
  - 1次时间 = 一次网络时间+一次命令时间 
  - N次时间 = n次网络时间+n次命令时间
  - 1次pipeline = 1次网络时间+n次命令 
- API
  - setbit key offset value：设置位图
  - getbit
  - bitcount key start end:获取位图指定的范围
  - bitop op destkey key [key…]：位操作
  - bitpos key tagetBit start end
- 实战
  - 独立用户统计
    - 1亿用户，5千万独立，独立用户更多时，bitmap更节省空间
    - 独立用户更少，则set更节省空间

### HyperLogLog

- 定义
  - 极小空间完成独立用户的统计
  - 本质还是字符串
  - 消耗内存极小
- API
  - pfadd key element [element...]
  - pfcount key [key...]
  - pfmerge destroy sourcekey [sourcekey...]
- 使用经验
  - 是否能容忍错误，官方错误率0.81%
  - 是否需要单条数据？不能取出来

### GEO

- 定义
  - 地理信息地位，存储经纬度，计算两地距离，范围计算等
- API
  - geoadd key longitude latitude member：增加地理位置
  - geopos key member：获取地理位置信息
  - geodist key member1 member2  [unit]
  - georadius：（较复杂）
- 应用场景
  - 微信摇一摇
  - 外卖

## Redis持久化的取舍和选择

### 持久化的作用

- 什么是持久化
  - redis将数据保存在内存中，对数据的更新将异步保存到磁盘中
- 实现方式
  - 快照：某时刻的所有数据
  - 写日志：操作日志

### RDB

- 什么是RDB
  - 二进制格式保存在硬盘中，redis重启后可以载入RDB文件到内存中
- 触发机制
  - save：同步，生成RDB文件，时间复杂度为O(n)
  - bgsave：异步，fork()，让子进程去生成RDB，同时还能响应客户端操作，时间复杂度为O(n)
  - 自动触发，生成RDB文件：满足条件
- 缺点
  - RDB耗时耗性能，时间复杂度为O(n)，bgsave的fork()会消耗内存，copy-on-write策略，IO性能消耗
  - 不可控，容易丢失数据

### AOF

- 原理
  - 日志原理，根据AOF的文件格式来定义
  - 将写命令写到缓冲区，然后根据策略刷新到硬盘中
- 策略
  - always：每条命令都进入硬盘，开销较大，不会丢失数据
  - everysec：每隔一秒写入：可能丢失一秒数据
  - no：由操作系统决定，不用管，但是不可控
  - AOF文件问题：
    - 文件大，恢复慢，硬盘性能有影响
    - 解决办法：AOF重写作用：减少硬盘占用量，加速恢复速度
    - 重写实现方式：
      - bgrewriteaof：fork出子进程，然后进行AOF重写，将redis的数据进行回溯，是redis的内存中进行
      - AOF重写配置：
        - auto-aof-rewrite-min-size和auto-aof-rewrite-percentage，对AOF当前尺寸进行统计，AOF上次启动和重写的尺寸
        - 当前尺寸大于最小尺寸，增长率大于重写增长率 
  - AOF配置：
    - appendonly yes
    - appendonlyfilename ""
    - appendfsync everysec
    - no-appendfsync-on-rewrite yes

### RDB和AOF的抉择

- RDB和AOF比较
  - RDB比AOF优先级低，体积比RDB大，但是恢复速度更快，RDB会丢失数据，而AOF根据策略决定丢失数据，相比来说RDB更重
- RDB最佳策略
  - ”关闭“RDB
  - 集中管理
  - 主从，从开？
- AOF最佳策略
  - “开”AOF：缓存和存储
  - AOF重写集中管理
  - everysec
- 最佳策略
  - 小分片
  - 缓存或者存储
  - 监控（硬盘，内存，负载，网络）
  - 足够的内存

## Redis复制的原理和优化

### runid和复制偏移量

- runid作为标识   

### 全量复制

- psync runid 偏移量
- FULLRESYNC
- save masterInfo
- bgsave
- send RDB
- send buffer
- flush old data
- load RDB

### 全量复制开销

- bgsave时间
- RDB文件网络传输时间
- 从节点清空数据时间
- 从节点加载RDB时间
- 可能的AOF重写时间

### 部分复制

- 由于抖动，导致部分数据丢失，但是写入了缓冲区buffer，默认为1M
- psync offset runId
- continue
- send partial data

### 故障处理

- 故障不可避免
- 自动故障转移
- slave故障
- master故障
  - master重新选取

### 开发与运维中遇到的问题

- 读写分离
  - 读流量分摊到从节点
  - 问题：
    - 复制数据延迟
    - 读到过期数据：懒惰策略以及定时任务
    - 从节点故障
- 主从配置不一致
  - maxmemory不一致：丢失数据
  - 数据结构优化参数：内存不一致
- 规避全量复制
  - 第一次全量复制
    - 第一次不可避免
    - 小主节点，低峰
  - 节点运行ID不匹配
    - 主节点重启，运行ID变化
    - 故障转移，例如哨兵或集群
  - 复制积压缓冲区不足
    - 网络中断，部分复制无法满足
    - 增大复制缓冲区配置rel_backlog_size，网络增强
- 规避复制风暴
  - 单主节点复制风暴：
    - 主节点重启，多从节点复制，解决办法：更换复制拓扑
  - 单机器复制缝补
    - 机器宕机，大量全量复制：主节点分散多机器

## Redis Sentinel

## Redis Cluster


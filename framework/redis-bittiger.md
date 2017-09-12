# Redis

## redis 操作

- 一般数据库中，如果将mykey -> list序列化存入redis中，这样不能修改某一个元素，需要将这个list拿出来，修改后，然后序列化存入，需要经过序列化反序列化。而redis可以对数据库内部进行操作，避免进行序列化，打通数据库和实际程序间的屏障。
- 支持的数据结构：Strings Hashes List Set SortedSet

## redis应用场景

- redis 使用情景：
  1. timeline，比如微信朋友圈，userID作为key对应一个list，作为缓存，机器down掉就没有了，redis支持很多复杂的数据结构，我们可以知道在permanent database支持多种数据结构是很少见的。
- redis实现：
  - 单线程的event loop，事件驱动。一个redis机器包含listening port，同时有内部周期性的事件，比如过期删除和定期persist，event loop则用来协调他们的工作。event loop：硬件，epoll注册事件，比如将一个端口注册到epoll，监听事件，查询的时候就知道已经连接好了。redis使用epoll来处理所有的文件接口事件
  - keep-alive并不是连接，只是定时ping解决问题
  - 设置tcp no delay，立刻发package
- 数据如何存在数据库中？
  - 存每个数的时候会存其数据结构
  - 所有的client连接都会放到hashtable中
  - 内部不同情况用不同的数据结构存储，即使对外还是简单的一种数据结构。
  - rehash，有个内部的任务在不断的进行一部分rehash操作。

## Pub／Sub

- pub会把消息发给所有的sub，但不是不保证稳定。

## persistence

- fork出子进程，fork不会进行内存拷贝
- RDB不会超过百分之五十
- AOF持久化 appen only log

## replication

master-slave

- slave----high available

## memeory blow up

- LRU cache

## cluster

- gossip protocol
- send request to a random serverhost

## migration




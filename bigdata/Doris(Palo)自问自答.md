## Doris自问自答

- _Doris系统架构_

  FE和BE,FE主要负责查询的编译,分发和元数据的管理(基于内存,类似于HDFS的NN),BE主要负责查询的执行和存储系统

- _Doris如何做谓词下推?_

- _Doris查询为什么快?_

  1. metadata(元数据)在内存中,元数据访问速度快;
  2. 聚合模型可以在导入时进行预聚合;
  3. 列式存储,只访问查询涉及的列,大量降低I/O
  4. Rollup预计算
  5. MPP查询引擎

- _Doris索引如何实现?_

  Mesa物理结构为:每个table按照大小拆分为data file,然后1024行数据会组合成一个row blocks,按照column存储,这里row blocks中key是按序存储的.

  每个data files都会有对应的index file,索引项为<key, value>,其中key为row block的第一个key,value是row blocks在data files中的offset.查找特定key的过程就是:先加载index文件,二分查找index文件获取包含特定key的row blocks的offset,然后从data file中获取指定的row blocks,最后在row blocks中二分查找特定的key

- _数据导入是如何执行的?_

  ![数据导入过程](http://static.zybuluo.com/kangkaisen/iaztr5b7lfu7oj7ohmyjae49/%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202018-04-18%20%E4%B8%8B%E5%8D%8811.29.48.png)

  ETL阶段:数据类型和格式的校验,根据Tablet拆分数据,按照key列进行排序,对Value进行聚合

  loading阶段:每个Tablet对应的BE拉取排序好的数据,进行数据的格式转换,生成索引	

  loading结束会进行元数据的更新

- _实时导入如何保证分布式事务?_

- _如何做HA?_

  **FE高可用**:Doris FE的高可用主要基于BerkeleyDB java version实现，BDB-JE实现了**类Paxos一致性协议算法**

  **BE高可用**: Doris会保证每个Tablet的多个副本分配到不同的BE上，所以一个BE down掉，不会影响查询的可用性

- _Doris查询计划是怎样的?_

  Mesa数据更新和数据版本化管理:为了获得高吞吐量,Mesa的数据更新是按照batch来更新的.为了在数据更新时不影响数据查询以及保证更新的原子性,Mesa采用了MVCC的方式,所以在数据更新时每个batch都需要指定一个version.

  由于Mesa采用MVCC,所以查询时也需要指定version,除此之外,也需要指定基于维度的predicate和查询的指标,为了回答对于特定version n的查询,Mesa需要聚合[0, n]范围内所有的deltas

  上述带来两个问题:存储成本增大,查询产生时延,因此需要及时删除不需要的过期的数据,同时将小文件merge为大的文件,减少IO,提高查询效率

  优点是无锁更新

  Mesa中引入了cumulative compaction和base compaction的概念。 每次更新,相同的key做预聚合,Mesa中将包含了一定版本的数据称为deltas, 表示为[V1, V2]，刚实时写入的小deltas， 称之为singleton deltas，然后每到一定的版本数，例如版本数为10，就通过cumulative compaction将10个singleton deltas合并为1个cumulative deltas，最终每天会通过base compaction将所有一定周期的内的deltas都合并为base deltas。 所以查询时只需要查询1个base deltas， 1个cumulative deltas和少数singleton deltas即可。

  注意，compaction是在后台并发和异步执行的，此外由于Mesa的存储是按照key有序存储的，所以deltas的merge是线性时间的。

  ![查询计划](http://static.zybuluo.com/kangkaisen/79z437wez74gf51x9ceec4ov/palo-impala.png)

  Doris的FE主要负责SQL的解析,语法分析,查询计划的生成和优化.查询计划的生成主要分为两步:

  1. 生成单节点查询计划(上图左下角);
  2. 将单节点的查询计划分布式化,生成PlanFragment(上图右半部分)

  第一步主要包括Plan Tree的生成，谓词下推， Table Partitions pruning，Column projections，Cost-based优化等；第二步 将单节点的查询计划分布式化，分布式化的目标是**最小化数据移动和最大化本地Scan**，分布式化的方法是增加ExchangeNode，执行计划树会以ExchangeNode为边界拆分为PlanFragment，1个PlanFragment封装了在一台机器上对同一数据集的部分PlanTree。如上图所示：各个Fragment的数据流转和最终的结果发送依赖：DataSink。

  当FE生成好查询计划树后，BE对应的各种Plan Node（Scan, Join, Union, Aggregation, Sort等）执行自己负责的操作即可。

- _Doris如何精确去重?_

  1. 按照所有的group by 字段和精确去重的字段进行聚合
2. 按照所有的group by 字段进行聚合

```sql
  SELECT a, COUNT(DISTINCT b, c), MIN(d), COUNT(*) FROM T GROUP BY a
-- 1st phase grouping exprs: a, b, c
  -- 1st phase agg exprs: MIN(d), COUNT(*)
-- 2nd phase grouping exprs: a
  -- 2nd phase agg exprs: COUNT(*), MIN(<MIN(d) from 1st phase>), SUM(<COUNT(*) from 1st phase>)
```

- _相比之下Kylin局限性在哪?_

  1. Kylin的核心思想是预计算,利用空间换时间来加速查询模式的OLAP查询;
2. Kylin主要满足离线固化多维分析需求,需要提前预定义维度和指标,然后查询时需要根据定义好的维度和指标进行查询,这样就无法满足即席查询的灵活多维分析需求,也没有保留明细数据,无法进行明细查询,比如任意字段聚合,不支持任意多表Join;
  3. 不支持online schema change
4. 部署不易,可维护性差(Doris直接部署FE和BE即可,而Kylin依赖于Hadoop生态,需要搭建Hive,Spark,HBase客户端)
  5. 维度属性变更需要重刷历史数据,代价过大.

  ![Kylin和Doris对比](https://blog.bcmeng.com/post/media/15545480639031/kylin-doris.png)

  

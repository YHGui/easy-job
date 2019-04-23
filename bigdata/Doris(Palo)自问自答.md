## Doris自问自答

- Doris系统架构

  FE和BE,FE主要负责查询的编译,分发和元数据的管理(基于内存,类似与HDFS的NN),BE主要负责查询的执行和存储系统

- Doris如何做谓词下推?

- Doris索引如何实现?

  Mesa物理结构为:每个table按照大小拆分为data file,然后几百行数据会组合成一个row blocks,按照column存储,这里row blocks中key是按序存储的.

  每个data files都会有对应的index file,索引项为<key, value>,其中key为row block的第一个key,value是row blocks在data files中的offset.查找特定key的过程就是:先加载index文件,二分查找index文件获取包含特定key的row blocks的offset,然后从data file中获取指定的row blocks,最后在row blocks中二分查找特定的key

- 数据导入是如何执行的?

  ETL阶段:数据类型和格式的校验,根据Tablet拆分数据,按照key列进行排序,对Value进行聚合

  loading阶段:每个Tablet对应的BE拉取排序好的数据,进行数据的格式转换,生成索引

  loading结束会进行元数据的更新

- 实时导入如何保证分布式事务?

- 如何做HA?

- Doris查询计划是怎样的?

  Mesa数据更新和数据版本化管理:为了获得高吞吐量,Mesa的数据更新是按照batch来更新的.为了在数据更新时不影响数据查询以及保证更新的原子性,Mesa采用了MVCC的方式,所以在数据更新时每个batch都需要指定一个version.

  由于Mesa采用MVCC,所以查询时也需要指定version,除此之外,也需要指定基于维度的predicate和查询的指标,为了回答对于特定version n的查询,Mesa需要聚合[0, n]范围内所有的deltas

  上述带来两个问题:存储成本增大,查询产生时延,因此需要及时删除不需要的过期的数据,同时将小文件merge为大的文件,减少IO,提高查询效率

  优点是无锁更新

  Mesa中引入了cumulative compaction和base compaction的概念。 每次更新,相同的key做预聚合,Mesa中将包含了一定版本的数据称为deltas, 表示为[V1, V2]，刚实时写入的小deltas， 称之为singleton deltas，然后每到一定的版本数，例如版本数为10，就通过cumulative compaction将10个singleton deltas合并为1个cumulative deltas，最终每天会通过base compaction将所有一定周期的内的deltas都合并为base deltas。 所以查询时只需要查询1个base deltas， 1个cumulative deltas和少数singleton deltas即可。

  注意，compaction是在后台并发和异步执行的，此外由于Mesa的存储是按照key有序存储的，所以deltas的merge是线性时间的。

- 相比之下Kylin局限性在哪?

  Kylin的核心思想是预计算,利用空间换时间来加速查询模式的OLAP查询

  不支持Join

  不支持online schema change

  可维护性差

  维度属性变更需要重刷历史数据,代价过大.




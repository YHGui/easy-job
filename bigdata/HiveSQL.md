## HiveSQL

1. 求出某天内首次登陆的人

```sql
select count(a.id)
from
(
    select id,
    	collect_set(time) as t
    from t_action_login
    where time <= '20150906'
    group by id
) as a
where size(a.t) = 1 and
	a.t[0] = '20150906'
```

2. 求出每门课程成绩前两名

```sql
select *
from (
	select cid,
		sid,
		grade,
		row_number() over (partition by cid order by grade desc) as rank
	from grade_tb
) tb
where rank < 2
```

3. 拉链表总结
   1. 拉链表首次运行,从今天开始用create_time作为link_begin_date,用20991231作为link_end_date;
   2. 以后每日例行工作,选择关注字段,如果发生变化则需要进行拉链存储
   3. step1:选取全量数据
   4. step2:最新的全量数据和拉链表里面的开链数据比较,找出变化的部分,做法为:将两张表进行full outer join,对比分为以下四类:
      1. 唯一标识身份的字段(比如uid,did等,同样也是做full outer join的字段)如果历史拉链表中没有,最新的表中有,则标志为"Insert";
      2. 如果历史拉链表有,最新的表没有,则标志为"Delete";
      3. 如果关注的字段(诸如属性字段)发生了变化,则标志为"Update";
      4. 其他的数据则说明没有变化,标志为"Remain".
   5. 处理已经封链的数据,原样输出
   6. Update和Delete的数据进行闭链操作
   7. Insert和Update的数据进行新建开链操作
   8. Remain标识未变的数据照样插入
   9. 上述5,6,7,8的数据全部union all,作为新的新的拉链表分区
4. count distinct优化,比如求三十日UV

```sql
select count(distinct uuid)
from server_stat tb
where date >= ${date - 30}
-- 这种情况下,数据倾斜是必然的,因为只有一个reducer在进行count distinct计算,所有数据都流向唯一的一个reducer
```

优化方法一:分治法,对uuid前n位进行group by,对uuid其他位数执行count distinct操作,最后对count distinct结果求和sum

```sql
select sum(uuid_num)
(
    select substr(uuid, 1, 3),
        count(distinct substr(uuid, 4)) as uuid_num
    from server_stat tb
    where date >= ${date - 30}
    group by substr(uuid, 1, 3)
) final;
```

优化方法二:

```sql
select sum(uuid_num) as total_num
(
    select tag,
        count(*) as uuid_num
    from 
    (
        select uuid,
            cast(rand() * 100 as bigint) as tag
        from  server_stat tb
        where date >= ${date - 30}
        group by uuid
    ) tb1
    group by tag
) final;
```

- 对uuid去重,并打tag
- 按照tag进行求和,统计每个tag下的uuid的个数
- 对所有的分组求和

5. Spark实现Word Count

```scala
val textfile = sc.textFile("hdfs_path")
val wordCount = textfile.flatMap(line => line.split(" ")).map(word => (word, 1)).reduceByKey((a, b) => a + b)
wordCount.collect()

uid, date计算uv
val textfile = sc.textFile("hdfs_path")
val textRDD = textfile.map(line => line.split(" ")(0) + "_" + line.split(" ")(1)).distinct().map(line => (line, 1)).reduceByKey(_ + _).map(item => item.swap).sortByKey(false).map(item => item.swap)
```

6. hive有个定时任务平时正常，没有啥问题，正常一般大概执行1个小时左右，但是今天突然报错了，报错代码：：running beyond physical memory limits. Current usage: 2.0 GB of 2 GB physical memory used; 3.9 GB of 4.2 GB virtual memory used. Killing container

   分析:container分配2g物理内存已经使用完了,虚拟内存4.2g已经使用了3.9g.
   
   1. 首先可以查看集群中每个container分配到内存的区间,我们是256M~8G.但是**具体每个container实际分配多少内存要看平台资源的负载情况,以及任务的优先级等因素**.通过将每个container的分配内存区间值调大的作用不是很大(**set yarn.scheduler.minimum-allocation-mb**,实际这个值要通过yarn-site.xml文件进行配置,并且需要重启集群才会生效)
   2. 查看知道mapTask和reduceTask可以使用的内存大小为集群设定值,所以如果是因为平台资源紧张造成部分任务因内存不够而失败(之前很多时候可以正常跑成功的任务),这是单方面调大**mapreduce.map.memory.mb和mapreduce.reduce.memory.mb**的值没有任何意义,因为尽管container可以分配的内存是1-8g,但现在因为集群资源紧张可能分配的内存小,那么即使调大map和reduce可使用内存也无济于事.不过平台资源丰富,调大其内存可见效.
   3. 增加虚拟内存的虚拟化比率
   4. 配置YARN不内存检查:可能造成内存泄露
   5. 增加map和reduce的个数:mapTask个数由文件大小,切片信息等确定,而reduce个数由最终需要聚合的结果决定,会产生多个小文件
      1. 增加map个数,通过设置切片信息,比如将切片大小改为64MB
      2. 增加reduce数量,通过设置参数(**hive.exec.reducers.bytes.per.reducer**)
   6. 优化程序
      1. 设置更多的重试次数,代价是消耗太大
      2. 检查数据倾斜情况,解决数据倾斜问题
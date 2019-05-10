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

1. 对uuid去重,并打tag
2. 按照tag进行求和,统计每个tag下的uuid的个数
3. 对所有的分组求和
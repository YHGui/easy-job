## HiveSQL

１. 求出某天内首次登陆的人

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


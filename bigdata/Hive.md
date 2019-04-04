# Hive

#### 背景

- 对HDFS或者HBase的表进行查询,需要手工写MapReduce代码,耗时耗力
- Hive基于统一的查询分析层,通过SQL语句的方式对HDFS上的数据进行查询,统计和分析
- Hive表是纯逻辑表,只是表的定义(元数据),本身不存储数据,完全依赖于HDFS和MapReduce

### 基础知识

- 内部表和外部表
  - 内部表 create table
  - 外部表 create external table,导入数据到外部表,数据并没有移动到自己的数据仓库目录下,而内部表则不一样,删除内部表的时候,Hive会把表的元数据和数据全部删除,而删除外部表的时候,Hive仅仅删除外部表的元数据,数据不会删除
- partition:辅助查询,缩小查询范围

### Hive优化

- 从一份基础表中按照不同维度,一次组合出不同的数据,可以使用如下语句:

  - from from_statement

    ​        insert overwrite table tablename1 
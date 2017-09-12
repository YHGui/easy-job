# 九章算法第0周

1. Implement strStr(http://www.lintcode.com/problem/strstr/

   描述：Returns the position of the first occurrence of string target in string source, or -1 if target is not part of source.

   解法：

   1. 边界情况：如果source和target两者有一者为null，return -1；
   2. source从0开始，直到第（source - target）个数，分别和target比较，这是外循环；
   3. 内循环source移动到第n位和target逐一比较，如果有不一致，跳出循环；
   4. 最后，得到和target全部一致，保险起见，看是否内循环的下标与target长度是否一致；

   时间复杂度：O(m*n)

2. subsets(http://www.lintcode.com/problem/subsets/)

   描述：求出一个集合所有子集

   解法：深度优先搜索解决，空间优化。搜索问题

   深度优先搜索：一路往下走，直到不能再往下走，为了避免重复（1 2 3， 1 3 2，2 1 3等），需要制定规则，这里就是按照升序添加。
## DFS

搜索的时间复杂度：`O(答案总数 * 构造每个答案的时间)`
举例：Subsets问题，求所有的子集。子集个数一共 2^n，每个集合的平均长度是 O(n) 的，所以时间复杂度为 O(n * 2^n)，同理 Permutations 问题的时间复杂度为：O(n * n!)

动态规划的时间复杂度：`O(状态总数 * 计算每个状态的时间复杂度)`
举例：triangle，数字三角形的最短路径，状态总数约 O(n^2) 个，计算每个状态的时间复杂度为 O(1)——就是求一下 min。所以总的时间复杂度为 O(n^2)

用分治法解决二叉树问题的时间复杂度：`O(二叉树节点个数 * 每个节点的计算时间)`
举例：二叉树最大深度。二叉树节点个数为 N，每个节点上的计算时间为 O(1)。总的时间复杂度为 O(N)

1. N皇后问题:将N个皇后放置在N*N的棋盘上,皇后彼此之间不能互相攻击,返回所有的解决方案

   ```Java
   class Solution {
       public List<List<String>> solveNQueens(int n) {
           List<List<String>> results = new ArrayList<List<String>>();
           if (n <= 0) {
               return results;
           }
           
           search(results, new ArrayList<Integer>(), n);
           return results;
       }
       
       private void search(List<List<String>> results, List<Integer> cols, int n) {
           //递归的出口
           if (cols.size() == n) {
               results.add(drawChessboard(cols));
               return;
           }
           //递归的拆解
           for (int colIndex = 0; colIndex < n; colIndex++) {
               if (!isValid(cols, colIndex)) {
                   continue;
               }
               cols.add(colIndex);
               search(results, cols, n);
               cols.remove(cols.size() - 1);
           }
       }
       
       private boolean isValid(List<Integer> cols, int column) {
           //cols中每个元素代表皇后的列索引,而cols这个List中的下标索引表示行索引
           int row = cols.size();
           for (int rowIndex = 0; rowIndex < cols.size(); rowIndex++) {
               //分别对比列向攻击
               if (cols.get(rowIndex) == column) {
                   return false;
               }
               //两个斜向攻击
               if (rowIndex + cols.get(rowIndex) == row + column) {
                   return false;
               }
               if (rowIndex - cols.get(rowIndex) == row - column) {
                   return false;
               }
           }
           return true;
       }
       
       //根据列皇后的位置来画出对应的棋盘
       private List<String> drawChessboard(List<Integer> cols) {
           List<String> chessboard = new ArrayList<>();
           for (int i = 0; i < cols.size(); i++) {
               //观察每一行
               StringBuilder sb = new StringBuilder();
               for (int j = 0; j < cols.size(); j++) {
                   //找出每一行中皇后所在的列
                   sb.append(j == cols.get(i) ? 'Q' : '.');
               }
               chessboard.add(sb.toString());
           }
           return chessboard;
       }
   }
   ```

2. 
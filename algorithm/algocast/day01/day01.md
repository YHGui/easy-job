## Day01

1. 有效回文串(valid-palindrome),leetcode 125,two pointers

   给定一个字符串,判断是否是回文字符串,字符串只考虑字母和数字,忽略字母大小写

   

   ```java
   class Solution {
       public boolean isPalindrome(String s) {
           if (s == null || s.length() == 0) {
               return true;
           }
           //left,right两根指针遍历整个字符串,每次移到字符串或者数字上,每次比较都相同则说明是回文串
           int left = 0;
           int right = s.length() - 1;
           while (left < right) {
               //left指针移动到第一个合法字符串
               while (left < s.length() && !isValid(s.charAt(left))) {
                   left++;
               }
               //如果整个字符串都为空字符串,也为回文串
               if (left == s.length()) {
                   return true;
               }
               //right指针移动到第一个合法字符串
               while (rigth > 0 && !isValid(s.charAt(right))) {
                   right--;
               }
               //将字符串统一化为小写比较
               if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
                   break;
               } else {
                   left++;
                   right--;
               }
           }
           //left指针移动到right指针右边或者两指针重合表明为回文串
           return left >= right;
       }
       //判断为字母和数字
       private boolean isValid(Char c){
           return Char.isLetter(c) || Char.isDigit(c);
       }
   }
   //时间复杂度为O(n), 空间复杂度为O(1)
   ```

1. 两数之和(two sum),leetcode 1, HashMap

   给定数组和目标数,求数组中两数之和为目标数的下标

   

   ```Java
   class Solution {
       public int[] twoSum(int[] nums, int target) {
           //用一个HashMap做辅助
           Map<Integer, Integer> map = new HashMap<Integer, Integer>();
           //将target-nums[i]作为key, i作为value,意为第i个数的互补的数作为key,i作为value,不存在则放入HashMap,等到互补的数出现,则将HashMap中的下标和当前下标输出即可
           for (int i = 0; i< nums.length; i++) {
               if (map.get(nums[i]) != null) {
                   int[] result = {map.get(nums[i]), i};
                   return result;
               }
               map.put(target - nums[i], i);
           }
           
           int[] result = {};
           return result;
       }
   }
   //时间复杂度为O(n), 空间复杂度为O(n)----HashMap辅助的
   ```

3. 有序数组两数之和(two sum II),leetcode 167, two pointers

   给定有序数组和目标数,求数组中两数之和为目标数的下标

   ```Java
   class Solution {
       public int[] twoSum(int[] numbers, int target) {
           int left = 0;
           int right = numbers.length - 1;
   
           while (left < right) {
               if (numbers[left] + numbers[right] > target) {
                   right--;
               } else if (numbers[left] + numbers[right] < target) {
                   left++;
               } else {
                   return new int[]{left + 1, right + 1};
               }
           }
   
           return new int[]{-1, -1};
       }
   }
   ```

4. 对称树判断(Symmetric Tree),leetcode101

   给定一棵二叉树,判断是否沿中轴线对称

   ```Java
   //递归版本
   class Solution {
       public boolean isSymmetric(TredNode root) {
           //判断左右子树是否对称
           return root == null || isSymmetricHelp(root.left, root.right);
       }
       
       public boolean isSymmetricHelp(TreeNode left, TreeNode right) {
           //左右子树有为空的一般为false
           if (left == null || right == null) {
               return left == right;
           }
           //左右子树根节点值不同,返回false
           if (left.val != right.val) {
               return false;
           }
           //判断左子树.左子树和右子树.右子树 && 左子树.右子树和右子树.左子树对称
           return isSymmetricHelp(left.left, right.right) && isSymmetricHelp(left.right, right.left);
       }
   }
   ```

   ```java
   //非递归版本
   class Solution {
       public boolean isSymmetric(TreeNode root) {
           if (root == null) {
               return true;
           }
           //申请辅助栈,将左右子树push到栈中
           Stack<TreeNode> stack = new Stack<TreeNode>();
           stack.push(root.left);
           stack.push(root.right);
   
           while (!stack.isEmpty()) {
               //pop出左右子树
               TreeNode right = stack.pop(), left = stack.pop();
               //左右子树均为null则continue
               if (left == null && right == null) {
                   continue;
               }
               if (left == null || right == null) {
                   return false;
               }
               if (left.val != right.val) {
                   return false;
               }
               stack.push(left.left);
               stack.push(right.right);
               stack.push(left.right);
               stack.push(right.left);
           }
           return true;
       }
   }
   ```

5. 不用+/-求两数之和(Sum of Two Integers),leetcode 371,异或,与

   

   ```Java
   //递归版本 a & b 是进位情况,a ^ b是不进位情况的相加
   Class Solution {
       public int getSum(int a, int b) {
           return b == 0 ? a : getSum(a^b, (a&b)<<1)
       }
   }
   ```

   ```Java
   //尾递归优化
   Class Solution {
       public int getSum(int a, int b) {
           while (b != 0) {
               int sum = a ^ b;
               int carry = (a & b) << 1;
               a = sum;
               b = carry;
           }
           return a;
       }
   }
   ```

6. 单身数(Single number),leetcode 136,异或/hashset

   ```Java
   Class Solution {
       public int singleNumber(int[] nums) {
           Set<Integer> set = new HashSet<>();
           int sum = 0, uniqueSum = 0;
           for (int num : nums) {
               if (!set.contains(num)) {
                   uniqueSum += num;
                   set.add(num);
               }
               sum += num;
           }
           return uniqueSum * 2 - sum;
       }
   }
   ```
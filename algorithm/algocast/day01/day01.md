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

7. 数据流中的中位数

思路: 用两个容器来分别存数据流序列中位数左右两边的数,只要保证左边容器的数都大于右边容器的数,且能在O(1)的时间分别取到左边容器的最小值和右边容器的最大值,因此分别借助小顶堆和大顶堆来存储数据,同时还需要保证左右容器相差不大于1,可以约定规则:偶数下标进入大顶堆.

```java
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class FindMedianNum {
    private int count = 0;
    private Queue<Integer> minHeap = new PriorityQueue<Integer>();
    private Queue<Integer> maxHeap = new PriorityQueue<Integer>(15, new Comparator<Integer>() {
        public int compare(Integer o1, Integer o2) {
            return o2 - o1;
        }
    });

    public void insert(Integer num) {
        if (count % 2 == 0) {
            // 当数据总数为偶数时,新加入的元素,应当加入小顶堆;但并不是直接进入小顶堆,而是经大顶堆筛选后取大顶堆最大元素进入小顶堆.
            // 1. 新加入的元素进入小顶堆,由大顶堆筛选出堆中最大元素
            maxHeap.offer(num);
            int maxNum = maxHeap.poll();
            // 2. 筛选后,即大顶堆中最大元素进入小顶堆
            minHeap.offer(maxNum);
        } else {
            // 当数据总数为奇数时,新加入的元素应该进入大顶堆;但并不是直接进入大顶堆,而是经小顶堆筛选后取小顶堆最小元素进入大顶堆.
            // 1. 新加入的元素进入小顶堆,由小顶堆筛选出堆中最小元素
            minHeap.offer(num);
            int minNum = minHeap.poll();
            // 2. 筛选后,即小顶堆中最小元素进入大顶堆
            maxHeap.offer(minNum);
        }
        count++;
    }

    public Double getMedian() {
        //分情况获取中位数
        if (count % 2 == 0) {
            return new Double((minHeap.peek() + maxHeap.peek()) * 1.0 / 2);
        } else {
            return new Double(minHeap.peek());
        }
    }
}
```

8. 一个链表奇数位上升序，偶数位上降序，不用额外空间让这个链表整体升序

比如: 1->8->3->6->5->4->7->2->9, 解题思路:1. 根据奇数位和偶数位拆分成两个链表;2. 将偶数位链表反转;3.将两个有序链表进行合并;

```java
public class SortedLinkedList {
    //1. 根据奇数位和偶数位拆分成两个链表
    private static Node[] divideList(Node head) {
        Node head1 = new Node(0);
        Node head2 = new Node(0);
        Node cur1 = head1;
        Node cur2 = head2;
        int cnt = 1;
        while (head != null) {
            if (cnt % 2 != 0) {
                cur1.next = head;
                cur1 = cur1.next;
            } else {
                cur2.next = head;
                cur2 = cur2.next;
            }
            head = head.next;
            cnt++;
        }
        cur1.next = null;
        cur2.next = null;
        return new Node[] {head1.next, head2.next};
    }

    //2. 将偶数位链表反转
    private static Node reverse(Node head) {
        if (head == null || head.next == null) {
            return head;
        }

        Node prev = null;
        while (head != null) {
            Node temp = head.next;
            head.next = prev;
            prev = head;
            head = temp;
        }

        return prev;
    }

    //3. 将两个有序链表进行合并
    private static Node combine(Node head1, Node head2) {
        if (head1 == null || head2 == null) {
            return head1 != null ? head1 : head2;
        }

        Node head = new Node(0);
        Node curt = head;
        while (head1 != null && head2 != null) {
            if (head1.value >= head2.value) {
                curt.next = head2;
                head2 = head2.next;
            } else {
                curt.next = head1;
                head1 = head1.next;
            }
            curt = curt.next;
        }

        if (head1 != null) {
            curt.next = head1;
        }

        if (head2 != null) {
            curt.next = head2;
        }
        return head.next;
    }

    public static void main(String[] args) {
        Node node1 = new Node(1);
        Node node2 = new Node(8);
        Node node3 = new Node(3);
        Node node4 = new Node(6);
        Node node5 = new Node(5);
        Node node6 = new Node(4);
        Node node7 = new Node(7);
        Node node8 = new Node(2);
        Node node9 = new Node(9);

        node1.next = node2;
        node2.next = node3;
        node3.next = node4;
        node4.next = node5;
        node5.next = node6;
        node6.next = node7;
        node7.next = node8;
        node8.next = node9;
        Node head = node1;

        Node[] lists = divideList(head);
        Node head1 = lists[0];
        Node head2 = lists[1];
        head2 = reverse(head2);
        head = combine(head1, head2);
        while (head != null) {
            System.out.println(head.value);
            head = head.next;
        }
    }
}

class Node {
    public int value;
    public Node next;

    Node(int value) {
        this.value = value;
    }
}
```

9. 丑数 leetcode 264, lintcode 4

```java
class Solution {
    public int nthUglyNumber(int n) {
        List<Integer> uglys = new ArrayList<Integer>();
        uglys.add(1);
        int p2 = 0, p3 = 0, p5 = 0;
        for (int i = 1; i < n; i++) {
            int lastNum = uglys.get(i - 1);
            while (uglys.get(p2) * 2 <= lastNum) p2++;
            while (uglys.get(p3) * 3 <= lastNum) p3++;
            while (uglys.get(p5) * 5 <= lastNum) p5++;

            uglys.add(Math.min(Math.min(uglys.get(p2) * 2, uglys.get(p3) * 3), uglys.get(p5) * 5));
        }
        
        return uglys.get(n - 1);
    }
}
```

10. Quick Select思路解决求第K大的数

```java
class Solution {
    public int kthLargestElement(int k, int[] nums) {
        if (nums == null || nums.length == 0) {
            return -1;
        }
        return quickSelect(nums, 0, nums.length - 1; k);
    }
    
    private int quickSelect(int[] nums, int start, int end, int k) {
        if (start == end) {
            return nums[start];
        }
        
        int left = start;
        int right = end;
        int pivot = nums[(left + right) / 2];
        while (left <= right) {
            while (left <= right && nums[left] > pivot) {
                left++;
            }
            while (left <= right && nums[right] < pivot) {
                right--;
            }
            if (left <= right) {
                int temp = nums[left];
                nums[left] = nums[right];
                nums[right] = temp;
                left++;
                right--;
            }
        }
        if (start + k - 1 <= right) {
            return quickSelect(nums, start, right, k);
        }
        if (start + k - 1 >= left) {
            return quickSelect(nums, left, end, k - (left - start));
        }
        
        return nums[right + 1];
    }
}
```

11.最大蓄水问题

```java
class Solution {
    public int maxArea(int[] height) {
        if (height == null || height.length <= 1) {
            return 0;
        }
        int left = 0;
        int right = height.length - 1;
        int max = Integer.MIN_VALUE;
        while (left < right) {
            max = Math.max(max, Math.min(height[left], height[right]) * (right - left));
            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }
        
        return max;
    }
}
```

12. 给出一个数组代表围柱的高度，求能围柱的最大的水量

```java
class Solution {
    public int maxArea(int[] height) {
        if (height == null || height.length < 2) {
            return 0;
        }
        int result = 0;
        int leftMax = 0;
        int rightMax = 0;
        int left = 0;
        int right = height.length - 1;
        while (left < right) {
            leftMax = Math.max(height[left], leftMax);
            rightMax = Math.max(height[right], rightMax);
            if (leftMax > rightMax) {
                result += (rightMax - height[right]);
                right--;
            } else {
                result += (leftMax - height[left]);
                left++;
            }
        }
        return result;
    }
}
```
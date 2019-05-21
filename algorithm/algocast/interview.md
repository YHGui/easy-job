1. 36进制加法

```Java
class Solution {
    public static String add36(String str1, String str2) {
      	String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int str1len = str1.length();
        int str2len = str2.length();
        int maxLen = Math.max(str1len, str2len);
        int inc = 0;
        int len = CHARS.length();
        String results = "";
        for (int i = 0; i < maxLen; i++) {
            int indexStr1 = i < str1len ? CHARS.indexOf(str1.charAt(str1len - i - 1)) : 0;
            int indexStr2 = i < str2len ? CHARS.indexOf(str2.charAt(str2len - i - 1)) : 0;
            System.out.println("str1: " + indexStr1 + " str2: " + indexStr2);
            int sum = indexStr1 + indexStr2 + inc;
            System.out.println("sum: " + sum);
            inc = sum / len;
            System.out.println("inc: " + inc);
            results = CHARS.charAt(sum % len) + results;
        }
        if (inc > 0) {
            results = CHARS.charAt(inc) + results;
        }
        return results;
    }
}
```


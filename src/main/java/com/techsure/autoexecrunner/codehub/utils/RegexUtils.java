package com.techsure.autoexecrunner.codehub.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

/**
 * @author yujh
 * 
 * 正则相关工具类
 *
 */
public class RegexUtils {
	/**
	 * 区分大小写的通配符匹配方法
	 * 
	 * @param pattern 匹配的模式
	 * @param str	准备匹配的字符串
	 * @return
	 */
	public static boolean wildcardMatch(String pattern, String str) {
		return FilenameUtils.wildcardMatch(str, pattern, IOCase.SENSITIVE);
	}
	
	
	// 下面是测试代码
    public static void main(String[] args) {
        test("*", "toto");
        test("toto.java", "tutu.java");
        test("12345", "1234");
        test("1234", "12345");
        test("*f", "");
        test("***", "toto");
        test("*.java", "toto.");
        test("*.java", "toto.jav");
        test("*.java", "toto.java");
        test("abc*", "");
        test("a*c", "abbbbbccccc");
        test("abc*xyz", "abcxxxyz");
        test("*xyz", "abcxxxyz");
        test("abc**xyz", "abcxxxyz");
        test("abc**x", "abcxxx");
        test("*a*b*c**x", "aaabcxxx");
        test("abc*x*yz", "abcxxxyz");
        test("abc*x*yz*", "abcxxxyz");
        test("a*b*c*x*yf*z*", "aabbccxxxeeyffz");
        test("a*b*c*x*yf*zze", "aabbccxxxeeyffz");
        test("a*b*c*x*yf*ze", "aabbccxxxeeyffz");
        test("a*b*c*x*yf*ze", "aabbccxxxeeyfze");
        test("*LogServerInterface*.java", "_LogServerInterfaceImpl.java");
        test("abc*xyz", "abcxyxyz");
  }

  private static void test(String pattern, String str) {
      System.out.println(pattern+" " + str + " =>> " + wildcardMatch(pattern, str));        
  }
}

package com.ty0207;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.util.Pair;

public class Main {


  public static void main(String[] args) {
    String str2 = new String("i");
    String m = str2.intern();
    String str1 = "i";
    System.out.println(str1 == str2);
    System.out.println(str1 == m);
    System.out.println(str2.intern() == str2);
    System.out.println(str1 == str2.intern());
    System.out.println(str1.intern() == str2.intern());
    System.out.println("--------");
    String str3 = new String("j");
    String str4 = "j";
    System.out.println(str4 == str3.intern());
    System.out.println(str4.intern() == str4);
    System.out.println("--------");
    final String ab = "ab";
    final String cd = "cd";
    String abcd = ab + cd;
    String tmp = "abcd";
    System.out.println(tmp == abcd);
    System.out.println("--------");
    String ef = "ef";
    String gh = "gh";
    String efgh = ef+gh;
    String tmp2 = "efgh";
    System.out.println(efgh == tmp2);
  }


}

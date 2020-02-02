package com.ty0207;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Main {

  public static class Test {

    public void println(String content) {
      System.out.println(content);
    }
  }
  public static void main(String[] args) throws Throwable {
    Test t = new Test();
    MethodType mt = MethodType.methodType(void.class, String.class);
    MethodHandles.lookup().findVirtual(t.getClass(), "println", mt).bindTo(t).invokeExact("fuck");
  }


}

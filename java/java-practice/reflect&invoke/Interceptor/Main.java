package com.ty0207;


public class Main {

  public static void main(String[] args) {
    Person dgy = new Student("dgy");
    InterceptorProxy proxy = new InterceptorProxy(new InterceptorImpl());
    Person dgyProxy = (Person) proxy.bind(dgy);
    //     Person dgyProxy = (Person) proxy.bind(dgy); error for com.sun.proxy.$Proxy0 cannot be cast to com.ty0207.Student
    System.out.println(dgyProxy.getName());
  }


}

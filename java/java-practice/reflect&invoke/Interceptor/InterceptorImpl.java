package com.ty0207;

import java.lang.reflect.Method;

public class InterceptorImpl implements Interceptor{

  @Override
  public boolean before(Object proxy, Object target, Method method, Object[] args) {
    System.out.println("before");
    return false;
  }

  @Override
  public void around(Object proxy, Object target, Method method, Object[] args) {
    System.out.println("around");
  }

  @Override
  public void after(Object proxy, Object target, Method method, Object[] args) {

    System.out.println("after");
  }

}

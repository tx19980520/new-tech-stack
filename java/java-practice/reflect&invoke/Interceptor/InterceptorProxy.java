package com.ty0207;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.NoSuchElementException;

public class InterceptorProxy implements InvocationHandler {
  private Object target = null;
  Interceptor interceptor = null;

  InterceptorProxy(Interceptor interceptor) {
    this.interceptor = interceptor;
  }

  public Object bind(Object target) {
    this.target = target;
    return Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(), this);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (interceptor == null) {
      method.invoke(target, args);
    }
    Object result = null;
    if (interceptor.before(proxy, target, method, args)) {
      throw new NoSuchElementException();
    } else {
      interceptor.around(proxy, target, method, args);
    }

    result = method.invoke(target, args);

    interceptor.after(proxy, target, method, args);

    return result;
  }

}


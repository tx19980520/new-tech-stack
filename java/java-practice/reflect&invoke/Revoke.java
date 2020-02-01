package com.ty0207;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


public class Main {
  public static class Person {

    Person() {
      name = "default";
      age = 0;
    }

    Person(String name, Integer age) {
      this.name = name;
      this.age = age;
    }

    private String name;
    private Integer age;

    public void setAge(Integer age) {
      this.age = age;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getAge() {
      return age;
    }

    public String getName() {
      return name;
    }
  }

  public static void main(String[] args)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
    Class<?> personClass = Person.class;
    Constructor<?> personClassConstructor = personClass.getDeclaredConstructor(String.class, Integer.class);
    Person dgy = (Person) personClassConstructor.newInstance("dgy", 22);
    Field[] fields = dgy.getClass().getDeclaredFields();
    for (Field field : fields) {
      System.out.println(field.getName());
      field.setAccessible(true);
      System.out.println(field.get(dgy));
    }
    Method[] methods = personClass.getMethods();
    for (Method method : methods) {
      System.out.print(method.getName() + " ");
      Class<?>[] parameters = method.getParameterTypes();
      for (Class<?> parameter : parameters) {
        System.out.print(parameter.getName() + " ");
      }
      System.out.println();
      if (method.getName().equals("getAge")) {
        System.out.printf("getAge from invoke: %d\n",method.getReturnType().cast(method.invoke(dgy)));
      }
    }
  }


}

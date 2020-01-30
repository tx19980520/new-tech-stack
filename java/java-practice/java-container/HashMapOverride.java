package com.ty0207;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Main {

  public static class Person {
    String name;
    Person(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()){
        return false;
      }
      Person person = (Person) obj;
      return this.name.equals(person.name);
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }
  }
  public static void main(String[] args) {
    Map<Person,String> persons = new HashMap<>();
    persons.put(new Person("yumi"), "bilili");
    String result = persons.get(new Person("yumi"));
    System.out.println(result);
  }


}

package com.ty0207;

public class Student implements Person {
  Student(String name) {
    this.name = name;
  }
  private String name;

  public String getName() {
    return name;
  }
}

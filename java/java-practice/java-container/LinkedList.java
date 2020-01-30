package com.ty0207;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class Main {

  public static void main(String[] args) {
    LinkedList<Integer> linkedList = new LinkedList<>();
    Deque<Integer> deque = linkedList;
    List<Integer> array = linkedList;
    linkedList.add(1);
    linkedList.add(2);
    linkedList.add(3);
    Iterator<Integer> itr = array.iterator();
    Iterator<Integer> itr2 = linkedList.iterator();
    while (itr2.hasNext()) {
      Integer result = itr2.next();
      if (result == 3) {
        linkedList.add(5);
        linkedList.remove();
      }
    }
//    while (itr.hasNext()) {
//      Integer result = itr.next();
//      if (result == 3) {
////        Integer last = deque.removeLast();
////        last++;
////        deque.addLast(last);
////          linkedList.add(5); //ConcurrentModificationException
////          linkedList.remove(); // nothing happens
//      }
//    }
    for (Integer i : linkedList) {
      System.out.println(i);
    }
  }


}

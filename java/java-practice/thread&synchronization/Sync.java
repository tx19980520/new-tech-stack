package com.ty0207;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.util.Pair;

public class Main {

  public static class Producer extends Thread {
    List<Integer> array;
    Random random;
    Producer(List<Integer> array) {
      this.array = array;
      random = new Random();
    }

    @Override
    public void run() {
      synchronized (array) {
        while (true) {
          if (array.isEmpty()) {
            Integer result = random.nextInt();
            array.add(result);
            System.out.printf("producer add %d\n", result);
            array.notify();
          } else {
            try {
              array.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }


    public static class Consumer extends Thread {
      List<Integer> array;
      Consumer(List<Integer> array) {
        this.array = array;
      }

    @Override
    public void run() {
      synchronized (array) {
        while(true) {
          if (array.isEmpty()) {
            try {
              array.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          } else {
            Integer result = array.remove(0);
            System.out.printf("consumer use %d\n", result);
            array.notify();
          }
        }
      }
    }
  }

  public static void main(String[] args) {
    List<Integer> array =  new ArrayList<>();
    Consumer c1 = new Consumer(array);
    Consumer c2 = new Consumer(array);
    Producer p1 = new Producer(array);
    Producer p2 = new Producer(array);
    c1.start();
    c2.start();
    p1.start();
    p2.start();
  }


}

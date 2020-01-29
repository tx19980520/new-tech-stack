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
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.util.Pair;

public class Main {

  public static class Producer extends Thread {

    private final String c;
    PipedOutputStream pos;
    Producer(PipedOutputStream pos, String c) {
      this.pos = pos;
      this.c = c;
    }

    @Override
    public void run() {
      while(true) {
        try {
          pos.write(c.getBytes());
          System.out.println(this.getName() + " make " + c);
        } catch (IOException e) {
          e.printStackTrace();
        }
        try {
          sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }


  public static class Consumer extends Thread {
    PipedInputStream pis;
    Consumer(PipedInputStream pis) {
      this.pis = pis;
    }

    @Override
    public void run() {
      while(true) {
        try {
          int count = pis.available();
          if (count > 0) {
            byte[] result = new byte[1];
            pis.read(result);
            System.out.println(this.getName() + " get "+  new String(result));
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        try {
          sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) {
    PipedOutputStream pos = new PipedOutputStream();
    PipedInputStream pis = new PipedInputStream();
    try {
      pis.connect(pos);
    } catch (IOException e) {
      e.printStackTrace();
    }

    Producer p1 = new Producer(pos, "a");
    Producer p2 = new Producer(pos, "b");
    p1.setName("p1");
    p2.setName("p2");
    Consumer c1 = new Consumer(pis);
    Consumer c2 = new Consumer(pis);
    c1.setName("c1");
    c2.setName("c2");
    p1.start();
    p2.start();
    c1.start();
    c2.start();
  }


}

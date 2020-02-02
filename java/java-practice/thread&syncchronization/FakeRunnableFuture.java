package com.ty0207;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

  public static class Worker implements Runnable {

    @Override
    public void run() {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println("hahaha");

    }
  }

  public static void main(String[] args) throws Throwable {
    ExecutorService threadPool = new ThreadPoolExecutor(12, 100, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
    Future<?> result = threadPool.submit(new Worker(), new Integer(1));
    Integer re = (Integer) result.get();
    System.out.println(re);
    threadPool.shutdown();
  }


}

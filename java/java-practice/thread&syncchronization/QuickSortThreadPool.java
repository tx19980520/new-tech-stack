package com.ty0207;

import com.sun.corba.se.spi.orbutil.threadpool.Work;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

  public static class Worker implements Runnable {
    private int start;
    private int end;
    private ExecutorService pool;
    private int[] array;
    Worker(int start, int end, int[] array) {
      this.start = start;
      this.end = end;
      this.array = array;
      pool = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(2));
    }

    @Override
    public void run() {
      System.out.printf("now start= %d, end = %d\n", start, end);
      int i = start;
      int j = end;
      if (j <= i) {
        return;
      }
      if (j - i == 1) {
        if (array[j] >= array[i]) {
          return;
        } else {
          int tmp = array[j];
          array[j] = array[i];
          array[i] = tmp;
          return;
        }
      }
      int mid = array[start];
      boolean iMove =false;
      while (i < j) {
        if (!iMove) {
          if (mid > array[j]) {
            array[i] = array[j];
            iMove = true;
          } else {
            j--;
          }
        } else {
          if (mid < array[i]) {
            array[j] = array[i];
            iMove = false;
          } else {
            i++;
          }
        }
      }
      array[i] = mid;
      if (end - start > 1) {
        pool.submit(new Worker(start, i-1, array));
        pool.submit(new Worker(i+1, end, array));
        try {
          pool.shutdown();
          while (!pool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
            System.out.printf("%s end: %d start: %d, i = %d\n", Thread.currentThread().getName(), end, start, i);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) throws Throwable {
    ExecutorService threadPool = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(2));
    int[] array = {0, 7, 3, 1, 6, 8, 2, 0};
    threadPool.submit(new Worker(0, array.length - 1, array));
    threadPool.shutdown();
    while (!threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS));
    System.out.println(Arrays.toString(array));
  }


}

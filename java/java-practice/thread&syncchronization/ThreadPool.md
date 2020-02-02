# ThreadPool

本文重点整理一下在JAVA中线程池的使用。

## 线程池的使用

### Runnable & Callable

Runnable和Callable都是用于在线程中被调用，相对不同的点在于，Runnable没有返回值，但是Callable是可以获取到返回值(Callback）。Callable可以抛出异常，在主线程进行处理，但是Runnable只能在try-catch在线程内部自行处理。

但实质上我们在ExecutorService中发现了如下的接口：

```java
<T> Future<T> submit(Runnable task, T result);
<T> Future<T> submit(Callable<T> task);
Future<?> submit(Runnable task);
```

在AbstractExecutorService中发现如下实现：

```java
public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task, result);
        execute(ftask);
        return ftask;
}

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
}
public Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<Void> ftask = newTaskFor(task, null);
        execute(ftask);
        return ftask;
}
```

你会发现这个最终追查下去，就还是回到了Callable似的封装，即便是不需要返回值。我们写了一些测试代码进行相关的测试。后来我们发现这个结果是个假的，原理上也可以接受，就是你放进去是什么值，他就是什么值。

repo中存在使用ThreadPool实现的快排。

#### case study

我们在快排时需要等到各级所有的线程完成工作再返回（可能merge sort对此的要求更高，因为是在子问题解决后再在父线程进行问题的处理）。

我们现在使用两种方法来解决这个问题。

##### shutdown & awaitTermination

```java
threadPool.shutdown();
while (!threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS));
```

各层级使用如上代码进行处理，但是注意这确实是一个两行代码，因此确实这个操作不是一个原子的操作，但是他其实是属于一个只读的操作，我们在`awaitTermination`内部是有看见一个上锁读ctl的操作。

##### CountDownLatch

是一个计数器，每次遇到线程完成工作，计数器就递减，最终直到计数器归零，这其实是独立于线程之外的一种实现方式。我并不知道这个函数调用能不能避免CPU乱序执行带来的困扰。
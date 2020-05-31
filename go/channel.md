# Channel 原理与使用规范

深入一下 channel 的实现并且了解一些 channel 的使用规范。

## Channel 相关细节

### 未使用 make 初始化的 Channel 将会造成 deadlock

```go
func main() {
    var x chan int
    go func() {
        x <- 1
    }()
    <-x
}
```

实际上返回的是一个指向channel的pointer，所以我们能够在不同的function之间直接传递channel对象，而不用通过指向channel的指针。

### 无缓存 Channel 和缓存 Channel 

无缓存 Channel 队列获取消息和发送消息必须是同步的，即发送端和接收端只要有一端不存在 goroutine 就会阻塞另一方。而缓存 Channel 只有在缓存满的情况下阻塞发送端，在缓存空的情况的下阻塞接受端。

### Channel 的关闭

重复关闭 Channel 会引发 panic。向关闭的 Channel 发送消息会引发 panic，但是接收关闭的 Channel 的消息不会引发 panic。因此一般是发送端负责使用 `close`关闭 Channel，接收端在接收时最好在接收消息时获取到第二个结果值，其为false则表示通道被关闭且通道中无元素值。

### 超时处理

一般情况接收端没有收到 Channel 中的内容时，将会一直阻塞。我们需要一些超时处理：

```go
select {
  case <- ch:
    // get data from ch
  case <- time.After(2 * time.Second)
    // read data from ch timeout
}
```

这样就能够避免 ch 一直阻塞的情况。

### 单向 Channel

Channel 默认情况下是双向的，我们一般会将双向 Channel 转换为单向 Channel，但是单向通道是不能够转换为双向通道的，我个人认为这主要是对程序员的一种约束，使得程序员能够在编译期就明确知晓自己对于 Channel 的错误使用，一般而言， 双向 Channel 没有约束谁是发送端，谁是接收端，在函数声明中明确这个事情，有利于我们思路变得更加的清晰。

## 源码分析

```go
type hchan struct {
	qcount   uint           // total data in the queue
	dataqsiz uint           // size of the circular queue
	buf      unsafe.Pointer // points to an array of dataqsiz elements
	elemsize uint16
	closed   uint32
	elemtype *_type // element type
	sendx    uint   // send index
	recvx    uint   // receive index
	recvq    waitq  // list of recv waiters
	sendq    waitq  // list of send waiters

	// lock protects all fields in hchan, as well as several
	// fields in sudogs blocked on this channel.
	//
	// Do not change another G's status while holding this lock
	// (in particular, do not ready a G), as this can deadlock
	// with stack shrinking.
	lock mutex
}

type waitq struct {
	first *sudog
	last  *sudog
}
```

这里主要注意的是buf，如果是一个缓冲 Channel，则buf的大小是大于1的，这时是使用循环队列的方式来实现先进先出，size的大小不能超过int32，不然会报错。

### Channel 状态分析

从消息队列，接收者链表，发送者链表三者的状态出发，分析接收者和发送者，进行排列组合。

**消息队列为空，接收者链表为空，发送者链表为空**

此时接收者如果接收消息，由于消息队列中无内容（必定接收者链表中同样为空），此时接收者将自己写入到 recq 之中，并且 recvx++，并且 gopark 调用将自己的 G 与 M  detach，设置为 waiting。发送者要发送消息，则直接将数据复制到消息队列中，然后return。

**消息队列为空，接收者链表为不为空，发送者链表为空**

此时过接收者接收消息，同上直接进入 recvq 并设置为 wait。如果是发送者，则是发现有接收者在等待，直接将元素复制到接收者的内存中，即 sudog 中的 element。

**消息队列不为空，接收者链表为空，发送者链表为空**

接收者直接从消息队列中拷贝一份数据消息队列更新其状态，发送者直接将消息写入到消息队列中。

**消息队列满，接收者链表为空，发送者链表为空**

此时接收者将会从消息队列中获取数据。发送者在消息队列满了之后将自己的 element 和写入了 sendq 中。

**消息队列满，接收者链表为空，发送者链表不为空**

此时接收者在获取到了消息队列中的信息之后，检查发现的发送者链表不为空，则会将发送者链表中的数据复制到消息队列中（注意之前在进入到发送者链表的时候已经发生了值拷贝），并且唤醒发送者（不然发送者会一直是在 waiting 的状态）。

其他的状态是不会出现在我们的状态机中的。
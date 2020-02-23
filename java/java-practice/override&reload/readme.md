## java的重载(overload)与重写(override)

## 静态类型&实际类型

```java
Human a = new Woman();
```

上述代码中Human是静态类型，Woman是实际类型（Woman是继承于Human），静态类型是在不会改变的，在编译期就可知，而实际类型会在runtime才可确定。

因此多出两种分派——静态分派和动态分派。

java中重载使用的是静态分派，即函数中参数的类型是依据静态类型进行选择的，如果遇到没有完全准确的函数可以使用，则java会选择“更加合适”的函数，并进行类型转换，或者在优先级相同的情况下爆出类型模糊(Type Ambiguous)，拒绝编译。

## 重载&重写

而对重写(override)而言，则是使用动态分派的方法，即，依靠实际类型进行选择，也就是我们所说多态的实现。

动态分派的基础原理就是在方法区创建以类为基本单位所形成的虚方法表(Virtual Method Table)，注意建立虚方法表是在解释执行时会被用到，在编译执行时会使用内联的方式来进行优化。虚方法表里面会存放各个方法的实际入口地址，如果子类没有对父类的方法覆写，则与父类指向同一入口。

在重载和重写都存在的情况下，先进行重写的判断，找到对应的实际类型的类，之后在实际类型的类里面找寻参数更加符合的方法，最终就能有你想要的答案。

重载和重写在java虚拟机翻译出来的字节码中，都会以`invokevirtual`的体现，`invokevirtual`的运行时解析的步骤如下：

1. 找到操作数栈顶元素的实际类型，即为C
2. 如果在类型C中找到相符合的Method，则进行访问权限校验，如果通过则返回这个方法的直接饮用，或返回`IllegalAccessError`的异常
3. 依照继承关系从下到上进行搜寻，如没有找到，抛出`AbstractMethodError`

这里疑惑的是如果是重载也是使用的`invokevirtual`指令，但是我们提到说重载是根据静态类型进行查找的，那么这里对于此，静态分配实质上再编译期已经确定好了执行的字节码，写入到了具体的位置，所以保证了不会出现“寻找的情况”（已经确定到最底部了）

重载对应的编码：

```powershell
		59: aload         5
        61: aload_1
        62: invokevirtual #11                 // Method Print:(Lcom/ty0207/Main$Human;)V
        65: aload         5
        67: aload_2
        68: invokevirtual #11                 // Method Print:(Lcom/ty0207/Main$Human;)V
        71: aload         5
        73: aload_3
        74: invokevirtual #12                 // Method Print:(Lcom/ty0207/Main$Woman;)V
```

重写对应的编码：

```powershell
		34: invokevirtual #6                  // Method com/ty0207/Main$Human.Print:()V
        37: aload_2
        38: invokevirtual #6                  // Method com/ty0207/Main$Human.Print:()V
        41: aload_3
        42: invokevirtual #7                  // Method com/ty0207/Main$Woman.Print:()V
```

重写对应的代码是需要进一步查找的。
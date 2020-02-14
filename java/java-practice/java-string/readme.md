# Java String

java的String不是原始类型，String 存在intern函数，intern函数是返回常量池中自身值的量，如果常量池中不存在该object，则选择把调用者放入常量池中，如果存在，则直接返回该常量池中的应用。

## 两种创建String的方法

`String str = new String("i");`

在两个不同的时期，创建了两个不同的对象，一个是在编译时期，进入到class文件中的静态常量池中，即在代码中出现的“i”，另外一次是在语句执行时，在堆中生成一个对象。

`String str = "i";`

在执行是首先进入到runtime常量池中进行查找，如果在runtime常量池中查找到相应的值，如果查找到，直接返回其对象的引用，如果没有找到，直接在runtime的常量池中创建该对象。

注意强调在静态常量池中的值完全不需要考虑。

```java
final String ab = "ab";
final String cd = "cd";
String abcd = ab + cd;
String tmp = "abcd";
System.out.println(tmp == abcd); // true
```

因为是final修饰，为常量，因此abcd的值是固定的（甚至再编译期会进行优化），此时abcd和tmp实质都是常量池中的同一个变量。

```java
String ef = "ef";
String gh = "gh";
String efgh = ef + gh;
String tmp2 = "efgh";
System.out.println(efgh == tmp2); // false
```

因为不是常量因此这里efgh在创建的时候并不进入到常量池，等同于进行new操作，因而不是相等的。
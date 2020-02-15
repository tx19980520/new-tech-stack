# java自定义注解

java的注解，从我们使用者的角度，其实就是附加的元信息，之后的事情会交给具体的类的实现进行处理。整体的流程就是如此，之后我们就将讨论对什么进行处理、如何处理以及在什么时候处理注解。

就什么时候处理注解，java这边有如下的时间节点：

```java
ElementType.TYPE：允许被修饰的注解作用在类、接口和枚举上

ElementType.FIELD：允许作用在属性字段上

ElementType.METHOD：允许作用在方法上

ElementType.PARAMETER：允许作用在方法参数上

ElementType.CONSTRUCTOR：允许作用在构造器上

ElementType.LOCAL_VARIABLE：允许作用在本地局部变量上

ElementType.ANNOTATION_TYPE：允许作用在注解上

ElementType.PACKAGE：允许作用在包上
```

这个部分比较好理解，主要是为了在编译阶段能够做相应的检查，以及相应的源信息能够落实到相应的结构体上。

```java
RetentionPolicy.SOURCE : 在编译阶段丢弃。这些注解在编译结束之后就不再有任何意义，所以它们不会写入字节。@Override, @SuppressWarnings都属于这类注解。

RetentionPolicy.CLASS : 在类加载的时候丢弃。在字节码文件的处理中有用。注解默认使用这种方式

RetentionPolicy.RUNTIME : 始终不会丢弃，运行期也保留该注解，因此可以使用反射机制读取该注解的信息。我们自定义的注解通常使用这种方式。
```

我们这里主要是考察在什么时候处理这个注解的相关情况的问题，我们这里主要讨论SOURCE和RUNTIME两种，RUNTIME的情况比较简单，其实主要是元信息的注册，我们可以在运行时用反射的方式获取到相关注解的值。

我们代码上首先要定义一个annotation，定义相关的类的信息即可。

```java
package com.ty0207;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RUNTIME)
@Documented
public @interface FruitName {
  String value() default "";
}
```

```java
package com.ty0207;

import java.lang.reflect.Field;

public class FruitUtils {
  public static void getFruitInfo(Class<?> clazz){

    String strFruitName=" 水果名称：";

    Field[] fields = clazz.getDeclaredFields();

    for(Field field :fields) {
      if(field.isAnnotationPresent(FruitName.class)) {
        FruitName fruitName = (FruitName) field.getAnnotation(FruitName.class);
        strFruitName = strFruitName + fruitName.value();
        System.out.println(strFruitName);
      }
    }
  }
}
```

整体来讲runtime的annotation比较简单，只是单纯的一个元信息附加。

这里我们讨论下，有关于SOURCE实现的用途。我们主要考察的是Lomlok里面有关于Getter/Setter的实现，我们可以通过Lomlok的相关注解，而减少写大量非常简单而冗长的代码。

首先我们需要创建一个Processor来处理对应的注解

```java
package com.mythsman.test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("com.mythsman.test.Getter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GetterProcessor extends AbstractProcessor {
    
    private Messager messager;
    private JavacTrees trees;
    private TreeMaker treeMaker;
    private Names names;
    
    
    private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
 		ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
 		statements.append(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())));
 		JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
 		return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getNewMethodName(jcVariableDecl.getName()), jcVariableDecl.vartype, List.nil(), List.nil(), List.nil(), body, null);
}

	private Name getNewMethodName(Name name) {
 		String s = name.toString();
 		return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
}

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(Getter.class);
    set.forEach(element -> {
        JCTree jcTree = trees.getTree(element);
        jcTree.accept(new TreeTranslator() {
            @Override
            public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                List<JCTree.JCVariableDecl> jcVariableDeclList = List.nil();

                for (JCTree tree : jcClassDecl.defs) {
                    if (tree.getKind().equals(Tree.Kind.VARIABLE)) {
                        JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) tree;
                        jcVariableDeclList = jcVariableDeclList.append(jcVariableDecl);
                    }
                }

                jcVariableDeclList.forEach(jcVariableDecl -> {
                    messager.printMessage(Diagnostic.Kind.NOTE, jcVariableDecl.getName() + " has been processed");
                    jcClassDecl.defs = jcClassDecl.defs.prepend(makeGetterMethodDecl(jcVariableDecl));
                });
                super.visitClassDef(jcClassDecl);
            }

        });
    });

    return true;
    }
}
```

我们这里观察init和process两个函数，init主要是获取到从当前的上下文（其实是在类中的信息）获取到语法书层面上的一些信息，因为我们后续会在process中会创建相应AST下的子树。

我们在process中首先找到所有被`@Getter`修饰的类，之后开始对每一个类进行遍历。

在每一个类中找到相应的字段，我们将字段进行记录。

之后我们对每一个VariableDec进行相关的工作，我们会在ClassDecl.defs中会添加相关Getter函数的definition和body。`makeGetterMethodDecl`这个函数里面主要是使用TreeMaker模拟语句，创建StatementList，再创建一个Method Def，主要是关于函数Access，Name，args，returnType。

感觉这个代码有一种写compiler的感觉，基本是在字节码上层一点点。


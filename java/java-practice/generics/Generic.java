package com.ty0207;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.util.Pair;

public class Main {


    private static class GenericTest<T> {
        Map<T,T> init;

        void doSomething() throws NoSuchFieldException {
            Class<?> class1 = this.init.getClass();
            Field f = class1.getField("init");
            ParameterizedType p = (ParameterizedType)f.getGenericType();
            Type[] types = p.getActualTypeArguments();
            for (int i = 0; i < types.length; i++) {
                System.out.println("第" + (i + 1) + "个泛型类型是：" + types[i]);
            }

        }
    }

    public static void main(String[] args) throws SecurityException,
        NoSuchFieldException {
        GenericTest<Integer> g = new GenericTest<Integer>();
        g.doSomething();
    }


}

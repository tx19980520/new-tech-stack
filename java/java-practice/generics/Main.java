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


    private Map<String, Integer> map = new HashMap<String, Integer>();

    public static void main(String[] args) throws SecurityException,
        NoSuchFieldException {
        // 获取Class实例
        Class<Main> class1 = Main.class;
        // 根据属性名取得该属性对应的Field对象
        Field mapField = class1.getDeclaredField("map");
        // 示范第一个方法：直接通过getType()取出Field的类型，只对普通类型的Field有效
        Class<?> class2 = mapField.getType();
        // 输出查看
        System.out.println("属性名为map的属性类型为：" + class2);

        // 示范第二种方法：
        Type mapMainType = mapField.getGenericType();
        // 为了确保安全转换，使用instanceof
        if (mapMainType instanceof ParameterizedType) {
            // 执行强制类型转换
            ParameterizedType parameterizedType = (ParameterizedType) mapMainType;
            // 获取基本类型信息，即Map
            Type basicType = parameterizedType.getRawType();
            System.out.println("基本类型为：" + basicType);
            // 获取泛型类型的泛型参数
            Type[] types = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < types.length; i++) {
                System.out.println("第" + (i + 1) + "个泛型类型是：" + types[i]);
            }
        } else {
            System.out.println("获取泛型类型出错!");
        }
    }


}

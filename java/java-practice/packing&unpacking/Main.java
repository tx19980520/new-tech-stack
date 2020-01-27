package com.ty0207;

import java.util.ArrayList;
import java.util.Stack;
import javafx.util.Pair;

public class Main {


    public static void main(String[] args) {
        Integer a = 1;
        Integer b = 2;
        Integer c = 3;
        Integer d = 3;
        Integer e = 321;
        Integer f = 321;
        Long g = 3L;
        System.out.println(c == d); // true 128 below has entered cache so the obj is the same
        System.out.println(e == f); // false
        System.out.println(c == (a+b)); // true use arithmetic comparison
        System.out.println(c.equals(a + b)); // true
        System.out.println(g == (a+b)); // true
        System.out.println(g.equals(a+b)); // false equals
    }

}

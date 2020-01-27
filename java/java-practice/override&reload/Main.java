package com.ty0207;

import java.util.ArrayList;
import java.util.Stack;
import javafx.util.Pair;

public class Main {


    public static void main(String[] args) {
        Human a = new Woman();
        Human b = new Man();
        Woman c = new Woman();
        Man d = new Man();
        a.Print(); // normal override
        b.Print(); // normal override
        c.Print(); // normal function call
        d.Print(); // normal function call
        Main m = new Main();
        m.Print(a);  // static load, so see the left type
        m.Print(b);  // static load, so see the left type
        m.Print(c);  // left type is Woman
        m.Print(d);  // left type is Man
        System.out.println(a.sex); // field use now static type, no override , pay attention to your view
        System.out.println(b.sex);
        System.out.println(c.sex);
        System.out.println(d.sex);
    }

    public abstract static class Human {
        public String sex = "none";
        public void Print() {
            System.out.println("I just know I'm a " + sex);
        }
    }

    public static class Woman extends Human {
        public String sex = "female";
        @Override
        public void Print() {
            System.out.println("I am a " + sex);
        }
    }

    public static class Man extends Human {
        public String sex = "male";
        @Override
        public void Print() {
            System.out.println("I am " + sex);
        }
    }

    public void Print(Man a) {
        System.out.println("Man channel");
    }

    public void Print(Human a) {
        System.out.println("Human channel");
    }

    public void Print(Woman a) {
        System.out.println("Woman channel");
    }

}

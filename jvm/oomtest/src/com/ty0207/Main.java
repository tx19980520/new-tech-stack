package com.ty0207;

import java.util.ArrayList;
import java.util.List;

public class Main {
    static class OOMObject {}

    public static void main(String[] args) {
        List<OOMObject>  l = new ArrayList<>();
        while (true) {
            l.add(new OOMObject());
        }
    }
}

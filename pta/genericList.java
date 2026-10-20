package pta;

import java.util.ArrayList;
import java.util.List;

public class genericList {
    void f1(List list) {}
    void f2(List<?> list) {}
    void f3(List<? extends B> list) {}
    void f4(List<B> list) {}

    List<C> listC = new ArrayList<>();
    List<B> listB = new ArrayList<>();

    public void main(String[] args) {
        // Both OK:
        // f1() accepts list of all types
        f1(listC);
        f1(listB);

        // Both OK
        f2(listC);
        f2(listB);

        // Both OK:
        // f3() accepts list of all types that extends B
        f3(listC);
        f3(listB);

        // OK
        f4(listB);
        // Error:
        // Though C extends B,
        // that doesn't indicate that List<C> extends List<B>
        f4(listC);
    }
}

class B {}

class C extends B {}

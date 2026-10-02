package pta;

class A {
    private int i = baz();

    public int baz() {
        System.out.print("A");
        return 0;
    }
}

class B extends A {
    private int i = baz();

    public int baz() {
        System.out.print("B");
        return 10;
    }
}

public class constructors {
    public static void main(String[] args) {
        A a = new B();
    }
}

// 父类对象构造时如果调用了被子类重写的方法，会调用子类版本！
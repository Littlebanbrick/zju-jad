package pta;

public class defaultConstructor {
    
}

class A {
    int i;
    A(int i) {    this.i = i * 2;    }
}

class B extends A {
    public static void main(String[] args) {
        B b = new B(2);
    }

    B(int x) {
        System.out.println(x);
    }
}

// 此代码无法编译。
// 原因是：子类B的构造函数没有显式调用父类A的构造函数，而父类A没有无参构造函数，
//      只有一个带参数的构造函数A(int i)。
// 在Java中，如果子类的构造函数没有显式调用父类的构造函数，编译器会尝试调用父类的无参构造函数。
// 如果父类没有无参构造函数，就会导致编译错误。
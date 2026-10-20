package pta;

// 在 Java 里，接口是一种引用数据类型，可以作为变量的类型。
interface Flyable {
    void fly();
}

class Bird implements Flyable {
    public void fly() {
        System.out.println("Bird flies");
    }
}

public class flyable {
    public static void main(String[] args) {
        Flyable f;              // 用接口作为变量类型
        f = new Bird();         // 指向实现了该接口的对象
        f.fly();                // 调用接口中声明的方法
    }
}
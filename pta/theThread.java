package pta;

public class theThread {
    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getPriority());
        System.out.println(Thread.currentThread().getName());

        new Thread(new Background()).start();
    }
}

class Background extends Thread {
    // 不能返回 int, 也不能没有 public - Thread 类中的 run() 方法签名如此
    public void run(){
        while(true){
            System.out.println("hello");
        }
    }
}

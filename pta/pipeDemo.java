package pta;

import java.io.*;

public class pipeDemo {
    public static void main(String[] args) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();

        // 连接输入管道和输出管道
        in.connect(out);
        // 或者创建时直接连接：
        // PipedInputStream in = new PipedInputStream(out);

        // 写线程
        Thread writer = new Thread(() -> {
            try {
                out.write("Hello from pipe".getBytes());
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 读线程
        Thread reader = new Thread(() -> {
            try {
                int data;
                while ((data = in.read()) != -1) {
                    System.out.print((char) data);
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        writer.start();
        reader.start();
    }
}

// 注意：数据是从 PipedOutputStream 写入，从 PipedInputStream 读出。
// 数据流向：PipedOutputStream -> PipedInputStream

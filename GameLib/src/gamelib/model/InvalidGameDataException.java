package gamelib.model;

// 为什么要自定义一个InvalidGameDataException异常类？
// 如果不与程序本身的bug区分开，JDK的报错也可能被误以为是数据不合法
// 新的异常类可以明确提示是数据不合法造成的错误，而非程序bug

public class InvalidGameDataException extends RuntimeException {

    // 序列化 = 把内存里的对象图变成字节序列
    // 反序列化 = 把字节序列变回内存里的对象图
    // Java中所有异常都可序列化
    //
    // 如果不声明 serialVersionUID，JVM会自动按类结构算一个
    // 此时若用不同的项目版本的类去反序列化就会报错：“代码改一行，存档全废”
    //
    // 一个long值，默认 1L 即可。除非故意作废旧存档/换新版本，否则不要改这个值
    private static final long serialVersionUID = 1L;

    public InvalidGameDataException(String message) {
        super(message);
    }

    public InvalidGameDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

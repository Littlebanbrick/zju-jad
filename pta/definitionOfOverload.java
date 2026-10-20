package pta;

public class definitionOfOverload {
    
}

// 简而言之，java中函数签名只包括方法名和参数列表，没有返回值！
// 所以函数签名冲突就是非法重载
class Myclass {
    // 原函数
    int someMethod(int x) {
        return x;
    }

    // 合法重载：参数个数不同
    int someMethod(int x, int y) {
        return x + y;
    }

    // 合法重载：参数类型不同
    int someMethod(double x) {
        return 0;
    }

    // 合法重载
    double someMethod(int x, int y, char z) {
        return 1.0;
    }

    // 非法重载：只有返回类型不同，会导致编译错误
    // definitionOfOverload.java:29: error: method someMethod(int) is already defined in class Myclass
    //     double someMethod(int x) {
    //            ^
    // 1 error
    // error: compilation failed
    double someMethod(int x) {
        return 1.0;
    }
}
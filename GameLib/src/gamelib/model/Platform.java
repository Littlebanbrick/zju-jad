package gamelib.model;

// enum 基本语法：
// public enum 枚举名 { 枚举值1, 枚举值2, ... }
//
// name() 方法返回枚举值的名字：Platform.STEAM.name() 返回 "STEAM"，是个字符串
// values() 方法返回枚举值的数组：Platform.values() 返回 [STEAM, EPIC, GOG, XBOX, OTHER]
//
// 这里 STEAM("Steam") 的写法等价于手写：
// public static final Platform STEAM = new Platform("Steam");

public enum Platform {

    STEAM("Steam"),
    EPIC("Epic"),
    GOG("GOG"),
    XBOX("Xbox"),
    OTHER("Other");     // 分号：常量列表结束

    private final String label;

    // 编译器自动private
    Platform(String label) {
        this.label = label;
    }

    // static 方法是属于类的，可以直接通过类名调用
    // 例如：Platform p = Platform.fromString("Steam");
    public static Platform fromString(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidGameDataException("平台不能为空，可选：" + choices());
        }

        // trim()去掉首尾空格
        String s = input.trim().toUpperCase();
        for (Platform p: values()) {
            if (p.name().equals(s) || p.label.toUpperCase().equals(s)) {
                return p;
            }
        }
        throw new InvalidGameDataException("未知平台：'" + input + "'，可选：" + choices());
    }

    public static String choices() {
        StringBuilder sb = new StringBuilder();
        for (Platform p: values()) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(p.label);
        }
        return sb.toString();
    }

    // override：重写。
    // Object.toString() 方法返回对象的字符串表示。
    // 如果不重写，Platform.STEAM.toString() 返回 "STEAM"
    // 这和 Platform.STEAM.name() 返回的结果一样，而我们额外需要一个能返回标签的方法
    @Override
    public String toString() {
        return label;
    }
}

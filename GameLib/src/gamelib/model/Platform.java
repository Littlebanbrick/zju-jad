package gamelib.model;

/**
 * 游戏所属的销售平台。
 *
 * <p>封闭集合，所以用 enum 而不是 String：拼错 {@code Platform.STAEM}
 * 直接编译不过。
 */
public enum Platform {

    STEAM("Steam"),
    EPIC("Epic"),
    GOG("GOG"),
    XBOX("Xbox"),
    OTHER("Other");

    /** 面向用户的显示名。 */
    private final String label;

    Platform(String label) {
        this.label = label;
    }

    /**
     * 把用户输入解析成 Platform，大小写和首尾空格都不敏感。
     *
     * <p>必须自己写：{@code Platform.valueOf("steam")} 会抛异常，因为 valueOf
     * 只认常量名（全大写），而且覆盖 toString() 不会改变 valueOf 接受什么
     * —— 这两个方法不互逆。
     *
     * @throws InvalidGameDataException 输入为空或不是已知平台
     */
    public static Platform fromString(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidGameDataException("平台不能为空，可选：" + choices());
        }
        String s = input.trim().toUpperCase();
        for (Platform p: values()) {
            if (p.name().equals(s) || p.label.toUpperCase().equals(s)) {
                return p;
            }
        }
        throw new InvalidGameDataException("未知平台：'" + input + "'，可选：" + choices());
    }

    /** 供提示信息使用，得到 "Steam/Epic/GOG/Xbox/Other"。 */
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

    /** 覆盖，返回显示名。 **/
    @Override
    public String toString() {
        return label;
    }
}
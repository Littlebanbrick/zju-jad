package gamelib.test;

import gamelib.model.Platform;

/** Platform 枚举的解析与显示。 */
public final class PlatformTest {

    private PlatformTest() {
    }

    public static void run(TestRunner t) {
        t.section("[1] Platform 解析与显示");

        t.check(Platform.fromString("steam") == Platform.STEAM, "fromString(\"steam\") 大小写不敏感");
        t.check(Platform.fromString(" Steam ") == Platform.STEAM, "fromString 容忍首尾空格");
        t.check(Platform.fromString("Epic") == Platform.EPIC, "fromString(\"Epic\") 按 label 匹配");
        t.check(Platform.fromString("Other") == Platform.OTHER, "fromString(\"Other\") 命中 OTHER");
        t.check(Platform.fromString("XBOX") == Platform.XBOX, "fromString(\"XBOX\") 按常量名匹配");
        t.check(Platform.GOG.toString().equals("GOG"), "toString 返回显示名");
        t.check(Platform.XBOX.name().equals("XBOX"), "name 返回常量标识符");
        t.check(Platform.values().length == 5, "共 5 个平台常量");
        t.check(Platform.choices().equals("Steam/Epic/GOG/Xbox/Other"), "choices() 生成提示文本");

        t.expectInvalid("fromString(\"Nintendo\")", () -> Platform.fromString("Nintendo"));
        t.expectInvalid("fromString(\"\")", () -> Platform.fromString(""));
        t.expectInvalid("fromString(\"   \")", () -> Platform.fromString("   "));
        t.expectInvalid("fromString(null)", () -> Platform.fromString(null));
    }
}

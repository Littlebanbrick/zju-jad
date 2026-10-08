package gamelib.test;

import gamelib.model.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
        t.check(Platform.choices().equals("Steam/Epic/GOG/Xbox/Other"), "choices() 生成提示文本");

        t.expectInvalid("fromString(\"Nintendo\")", () -> Platform.fromString("Nintendo"));
        t.expectInvalid("fromString(\"\")", () -> Platform.fromString(""));
        t.expectInvalid("fromString(\"   \")", () -> Platform.fromString("   "));
        t.expectInvalid("fromString(null)", () -> Platform.fromString(null));

        testValues(t);
        testEnumSet(t);
    }

    private static void testValues(TestRunner t) {
        t.section("[1b] values()：遍历全部平台");

        Platform[] all = Platform.values();

        t.check(all.length == 5, "values() 返回 5 个平台");
        t.check(all[0] == Platform.STEAM, "首个是 STEAM（保持声明顺序）");
        t.check(all[1] == Platform.EPIC, "第二个是 EPIC");
        t.check(all[4] == Platform.OTHER, "末个是 OTHER");

        t.check(Platform.values() != Platform.values(), "★ values() 每次返回新数组");
        Platform[] scratch = Platform.values();
        scratch[0] = null;
        t.check(Platform.values()[0] == Platform.STEAM, "★ 改这个副本不影响枚举本体");

        int count = 0;
        boolean sawXbox = false;
        for (Platform p : Platform.values()) {
            count++;
            if (p == Platform.XBOX) {
                sawXbox = true;
            }
        }
        t.check(count == 5, "for-each 迭代到 5 个");
        t.check(sawXbox, "迭代过程中能看到 XBOX");

        t.check(Arrays.asList(Platform.values()).indexOf(Platform.GOG) == 2,
                "Arrays.asList(values()).indexOf(GOG) == 2");
        List<Platform> copy = new ArrayList<>(Arrays.asList(Platform.values()));
        copy.remove(Platform.STEAM);
        t.check(copy.size() == 4, "想改就自己包一个可变副本");
        t.check(Platform.values().length == 5, "★ 改副本不影响 values() 本身");

        t.expectThrows(NullPointerException.class,
                "★ 反面对照：List.of(values()).contains(null) 抛 NPE",
                () -> List.of(Platform.values()).contains(null));
    }

    private static void testEnumSet(TestRunner t) {
        t.section("[1c] EnumSet：需要成员判断时用它");

        Set<Platform> set = EnumSet.allOf(Platform.class);

        t.check(set.size() == 5, "EnumSet.allOf 含 5 个元素");
        t.check(set.contains(Platform.GOG), "成员判断命中");
        t.check(set.contains(Platform.OTHER), "OTHER 也在集合里");
        t.check(!set.contains(null), "★ contains(null) 返回 false 而不是抛 NPE");
        t.check(!set.contains("Steam"), "★ 传入 String 也返回 false，不抛异常");
        t.check(set.toString().equals("[Steam, Epic, GOG, Xbox, Other]"),
                "EnumSet 迭代顺序同样是声明顺序");

        Set<Platform> owned = EnumSet.noneOf(Platform.class);
        owned.add(Platform.STEAM);
        owned.add(Platform.EPIC);
        t.check(owned.size() == 2, "EnumSet.noneOf + add 可以只装拥有的平台");
        t.check(EnumSet.complementOf(EnumSet.of(Platform.STEAM)).size() == 4,
                "complementOf 求补集（没买过的平台）");
    }
}

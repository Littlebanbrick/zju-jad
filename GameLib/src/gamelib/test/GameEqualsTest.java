package gamelib.test;

import gamelib.model.Game;
import gamelib.model.Platform;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * equals / hashCode / key 契约。
 *
 * <p>这一组是 Game 里最容易埋雷的部分：写错了不会编译报错，
 * 只会在 HashMap / ArrayList 内部静默失效。
 */
public final class GameEqualsTest {

    private GameEqualsTest() {
    }

    public static void run(TestRunner t) {
        t.section("[5] equals / hashCode 契约");

        Game a = new Game("Elden Ring", Platform.STEAM, "ARPG", 298.0,
                LocalDate.of(2022, 3, 1), 10, 120.0, "");
        Game b = new Game("  elden ring  ", Platform.STEAM, "RPG", 0.0,
                LocalDate.of(2023, 1, 1), 1, 5.0, "另一份记录");

        t.check(a.equals(b), "标题大小写/空格不同但 key 相同 -> equals 为 true");
        t.check(b.equals(a), "对称性：b.equals(a) 也为 true");
        t.check(a.hashCode() == b.hashCode(), "equals 相等则 hashCode 必须相等");
        t.check(a.equals(a), "自反性：a.equals(a) 为 true");

        Game c = new Game("Elden Ring", Platform.EPIC, "ARPG", 0.0,
                LocalDate.of(2022, 3, 1), 10, 120.0, "");
        t.check(!a.equals(c), "同标题不同平台 -> 不等");
        t.check(a.key().equals("elden ring|STEAM"), "key() = 规范化标题 + name()");
        t.check(!a.equals(null), "equals(null) 为 false 且不抛 NPE");
        t.check(!a.equals("Elden Ring"), "与不相关类型比较 -> false");
        t.check(a.equals(b) && a.key().equals(Game.keyOf("ELDEN RING", Platform.STEAM)),
                "keyOf 与 key 算法一致");

        t.section("[6] 改了业务字段后，容器里仍必须能找到它");

        a.addPlaytime(5.0);
        t.check(a.getPlaytimeHours() == 125.0, "addPlaytime(5) 累加到 125h");
        t.check(a.equals(b), "改时长后 equals 仍为 true（契约稳定性）");
        t.check(a.hashCode() == b.hashCode(), "改时长后 hashCode 不变");

        List<Game> list = new ArrayList<>();
        list.add(a);
        t.check(list.contains(b), "ArrayList.contains 靠 equals 找到它");
        t.check(list.indexOf(b) == 0, "indexOf 返回 0");
        t.check(list.remove(b), "ArrayList.remove 能删掉它");
        t.check(list.isEmpty(), "删除后列表为空");

        Set<Game> set = new HashSet<>();
        set.add(a);
        set.add(b);
        t.check(set.size() == 1, "HashSet 按 key 去重，只留 1 个");

        t.check(!a.equals(null), "equals(null) 再次确认为 false");
    }

    /** 模拟第 2 步 store 层的查重路径：Map<String, Game> + keyOf。 */
    public static void runStorePreview(TestRunner t) {
        t.section("[7] 预告：第 2 步的 Map 索引形状");

        Game a = new Game("The Witcher 3", Platform.GOG, "RPG", 99.0,
                LocalDate.of(2020, 6, 1), 9, 60.0, "");
        Game b = new Game("The Witcher 3", Platform.STEAM, "RPG", 158.0,
                LocalDate.of(2021, 1, 1), 9, 60.0, "");

        Map<String, Game> lib = new HashMap<>();
        lib.put(a.key(), a);
        lib.put(b.key(), b);

        t.check(lib.size() == 2, "同标题不同平台是两款游戏（§2 「各买了一次」）");
        t.check(lib.containsKey(Game.keyOf("THE WITCHER 3", Platform.GOG)),
                "大小写不敏感地查到 GOG 那份");
        t.check(!lib.containsKey(Game.keyOf("The Witcher 3", Platform.EPIC)),
                "没买过 Epic 那份 -> 查不到");
    }
}

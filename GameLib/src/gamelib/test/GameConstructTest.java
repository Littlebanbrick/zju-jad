package gamelib.test;

import gamelib.model.Game;
import gamelib.model.Platform;

import java.time.LocalDate;

/** Game 的构造、边界值与输入校验。 */
public final class GameConstructTest {

    private GameConstructTest() {
    }

    public static void run(TestRunner t) {
        t.section("[2] 构造与字段读取");

        Game elden = new Game(
                "Elden Ring", 
                Platform.STEAM, 
                "ARPG", 
                298.0,
                LocalDate.of(2022, 3, 1), 
                10, 
                120.0, 
                "神作，DLC快出！"
        );
        t.check(elden.getTitle().equals("Elden Ring"), "标题正确");
        t.check(elden.getPlatform() == Platform.STEAM, "平台正确");
        t.check(elden.getGenre().equals("ARPG"), "类型正确");
        t.check(elden.getRating() == 10, "评分上限 10 合法");
        t.check(elden.getPurchasePrice() == 298.0, "价格正确");
        t.check(elden.getPurchaseDate().equals(LocalDate.of(2022, 3, 1)), "购买日期正确");
        t.check(elden.getPlaytimeHours() == 120.0, "时长正确");
        t.check(elden.getNotes().equals("神作，DLC快出！"), "中文笔记不在 I/O 处乱码");
        t.check(!elden.isCompleted(), "新建游戏默认未通关");
        t.check(elden.getCompletedDate() == null, "初始 completedDate 为 null");
        t.check(!elden.isHidden(), "新建游戏默认未隐藏");
        System.out.println("    打印验证：" + elden);

        t.section("[3] 边界值：闭区间两端与今天必须通过");

        Game free = new Game("GTA5", Platform.EPIC, "Action", 0.0,
                LocalDate.now(), 0, 0.0, null);
        t.check(free.getPurchasePrice() == 0.0, "价格 0 合法（§2 的 Epic 白嫖场景）");
        t.check(free.getRating() == 0, "评分下限 0 合法");
        t.check(free.getPurchaseDate().equals(LocalDate.now()), "购买日期 = 今天合法");
        t.check(free.getPlaytimeHours() == 0.0, "时长 0 合法");
        t.check(free.getNotes().isEmpty(), "note 传 null 自动变成空串");

        Game padded = new Game("  Hollow Knight  ", Platform.STEAM, "  Metroidvania  ",
                50.0, LocalDate.of(2021, 5, 1), 9, 30.0, "");
        t.check(padded.getTitle().equals("Hollow Knight"), "标题首尾空格被 trim");
        t.check(padded.getGenre().equals("Metroidvania"), "类型首尾空格被 trim");

        t.section("[4] 非法输入必须抛 InvalidGameDataException");

        t.expectInvalid("负价格", () -> new Game("A", Platform.STEAM, "RPG", -1.0,
                LocalDate.of(2020, 1, 1), 5, 10.0, ""));
        t.expectInvalid("未来购买日期", () -> new Game("A", Platform.STEAM, "RPG", 1.0,
                LocalDate.now().plusDays(1), 5, 10.0, ""));
        t.expectInvalid("评分 11", () -> new Game("A", Platform.STEAM, "RPG", 1.0,
                LocalDate.of(2020, 1, 1), 11, 10.0, ""));
        t.expectInvalid("评分 -1", () -> new Game("A", Platform.STEAM, "RPG", 1.0,
                LocalDate.of(2020, 1, 1), -1, 10.0, ""));
        t.expectInvalid("空白标题", () -> new Game("   ", Platform.STEAM, "RPG", 1.0,
                LocalDate.of(2020, 1, 1), 5, 10.0, ""));
        t.expectInvalid("null 标题", () -> new Game(null, Platform.STEAM, "RPG", 1.0,
                LocalDate.of(2020, 1, 1), 5, 10.0, ""));
        t.expectInvalid("null 平台", () -> new Game("A", null, "RPG", 1.0,
                LocalDate.of(2020, 1, 1), 5, 10.0, ""));
        t.expectInvalid("空白类型", () -> new Game("A", Platform.STEAM, "", 1.0,
                LocalDate.of(2020, 1, 1), 5, 10.0, ""));
        t.expectInvalid("负时长", () -> new Game("A", Platform.STEAM, "RPG", 1.0,
                LocalDate.of(2020, 1, 1), 5, -1.0, ""));
        t.expectInvalid("null 购买日期", () -> new Game("A", Platform.STEAM, "RPG", 1.0,
                null, 5, 10.0, ""));
    }
}

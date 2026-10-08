package gamelib.test;

import gamelib.model.Game;
import gamelib.model.Platform;

import java.time.LocalDate;

public final class GameStateTest {

    private GameStateTest() {
    }

    public static void run(TestRunner t) {
        t.section("[8] 通关状态（方案 B：completedDate 非 null 即已通关）");

        Game g = new Game("The Witcher 3", Platform.GOG, "RPG", 99.0,
                LocalDate.of(2020, 6, 1), 9, 60.0, "剧情神作, DLC别买");

        g.markCompleted();
        t.check(g.isCompleted(), "markCompleted() 后 isCompleted 为 true");
        t.check(g.getCompletedDate() != null, "completedDate 被盖章");
        t.check(g.getCompletedDate().equals(LocalDate.now()), "盖章值 = 今天");
        t.check(g.getCompletedDate().getYear() == LocalDate.now().getYear(),
                "§5 年度通关数能读出年份");

        LocalDate first = g.getCompletedDate();
        g.markCompleted();
        t.check(g.getCompletedDate().equals(first), "重复 markCompleted() 不覆盖原日期（幂等）");

        t.section("[9] 补录历史通关日期（第 3 步 CSV 导入的路径）");

        Game h = new Game("Hades", Platform.STEAM, "Roguelike", 70.0,
                LocalDate.of(2021, 1, 1), 9, 40.0, "");
        h.markCompleted(LocalDate.of(2024, 3, 15));
        t.check(h.isCompleted(), "补录后已通关");
        t.check(h.getCompletedDate().getYear() == 2024, "可补录 2024 年的通关日期");
        t.check(h.getCompletedDate().equals(LocalDate.of(2024, 3, 15)), "日期完整保留");

        t.section("[10] 业务方法的校验");

        Game v = new Game("B", Platform.STEAM, "RPG", 1.0,
                LocalDate.of(2020, 1, 1), 5, 10.0, "");
        t.expectInvalid("updateRating(99)", () -> v.updateRating(99));
        t.expectInvalid("updateRating(-1)", () -> v.updateRating(-1));
        t.expectInvalid("addPlaytime(-1)", () -> v.addPlaytime(-1));
        t.expectInvalid("markCompleted(null)", () -> v.markCompleted(null));
        t.expectInvalid("通关日期早于购买日期", () -> v.markCompleted(LocalDate.of(2019, 1, 1)));
        t.expectInvalid("通关日期在未来", () -> v.markCompleted(LocalDate.now().plusDays(1)));
        t.expectInvalid("keyOf 空白标题", () -> Game.keyOf("  ", Platform.STEAM));
        t.expectInvalid("keyOf null 平台", () -> Game.keyOf("A", null));

        v.addPlaytime(2.0);
        t.check(v.getPlaytimeHours() == 12.0, "addPlaytime(2) 累加到 12h");
        v.updateRating(8);
        t.check(v.getRating() == 8, "updateRating(8) 生效");

        t.section("[11] 笔记与隐藏（§4.4 扩展）");

        v.setNotes("  通关后觉得值 9 分  ");
        t.check(v.getNotes().equals("通关后觉得值 9 分"), "setNotes 覆盖并 trim");
        v.setNotes(null);
        t.check(v.getNotes().isEmpty(), "setNotes(null) 变成空串");

        Game copy = new Game("The Witcher 3", Platform.GOG, "RPG", 1.0,
                LocalDate.of(2021, 1, 1), 1, 1.0, "");
        g.hide();
        t.check(g.isHidden(), "hide() 生效");
        t.check(g.equals(copy), "隐藏状态不参与 equals");
        t.check(g.isCompleted(), "隐藏不影响通关状态");
        g.unhide();
        t.check(!g.isHidden(), "unhide() 生效");
    }
}

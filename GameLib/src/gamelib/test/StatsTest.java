package gamelib.test;

import gamelib.model.Game;
import gamelib.model.Platform;
import gamelib.store.GameLibrary;
import gamelib.ui.Stats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StatsTest {

    private StatsTest() {
    }

    public static void run(TestRunner t) {
        testEmpty(t);
        testAggregation(t);
        testOrder(t);
        testHidden(t);
        testMostExpensivePlatform(t);
        testRatingZero(t);
    }

    private static void testEmpty(TestRunner t) {
        t.section("[30] Stats：空库");

        Stats s = Stats.of(new GameLibrary());

        t.check(s.totalGames() == 0, "totalGames = 0");
        close(t, "totalSpent = 0.0", s.totalSpent(), 0.0);
        t.check(!Double.isNaN(s.averageRating()), "★ averageRating 不是 NaN");
        close(t, "★ averageRating = 0.0", s.averageRating(), 0.0);
        t.check(s.countByPlatform().isEmpty(), "平台数量表为空");
        t.check(s.spendByPlatform().isEmpty(), "平台花费表为空");
        t.check(s.completedCount() == 0, "completedCount = 0");
        t.check(s.unfinishedCount() == 0, "unfinishedCount = 0");
        t.check(s.isEmpty(), "isEmpty() 为 true");
        t.check(s.mostExpensivePlatform().isEmpty(),
                "★ 空库没有「最烧钱的平台」-> Optional.empty()");

        t.expectThrows(NullPointerException.class, "of(null) 抛 NPE",
                () -> Stats.of(null));
    }

    private static void testAggregation(TestRunner t) {
        t.section("[31] Stats：基本聚合");

        Stats s = Stats.of(fixture());

        t.check(s.totalGames() == 4, "totalGames = 4");
        close(t, "totalSpent = 467.0", s.totalSpent(), 467.0);
        close(t, "averageRating = 9.0", s.averageRating(), 9.0);
        t.check(!s.isEmpty(), "isEmpty() 为 false");

        t.check(s.countByPlatform().get(Platform.STEAM) == 2, "Steam 2 款");
        t.check(s.countByPlatform().get(Platform.EPIC) == 1, "Epic 1 款");
        t.check(s.countByPlatform().get(Platform.GOG) == 1, "GOG 1 款");
        t.check(s.countByPlatform().size() == 3, "★ 只含买过的平台，不含 0 款的 Xbox/Other");

        close(t, "Steam 共花 368.0", s.spendByPlatform().get(Platform.STEAM), 368.0);
        close(t, "Epic 共花 0.0（白嫖）", s.spendByPlatform().get(Platform.EPIC), 0.0);
        close(t, "GOG 共花 99.0", s.spendByPlatform().get(Platform.GOG), 99.0);

        t.check(s.completedCount() == 3, "已通关 3 款");
        t.check(s.unfinishedCount() == 1, "未通关 1 款");
        t.check(s.completedCount() + s.unfinishedCount() == s.totalGames(),
                "已通关 + 未通关 == 总数");

        t.check(s.completedByYear().get(2023) == 1, "2023 年通关 1 款");
        t.check(s.completedByYear().get(2024) == 2, "2024 年通关 2 款");
        t.check(s.completedByYear().size() == 2, "★ 只含真有通关记录的年份");
        t.check(!s.completedByYear().containsKey(2022), "没有 2022 年的记录");
    }

    private static void testOrder(TestRunner t) {
        t.section("[32] Stats：迭代顺序必须稳定");

        Stats s = Stats.of(fixture());

        List<Platform> platformOrder = new ArrayList<>(s.countByPlatform().keySet());
        t.check(platformOrder.size() == 3, "3 个平台");
        t.check(platformOrder.get(0) == Platform.STEAM, "★ 首个平台是 STEAM");
        t.check(platformOrder.get(1) == Platform.EPIC, "★ 第二个是 EPIC");
        t.check(platformOrder.get(2) == Platform.GOG, "★ 第三个是 GOG");

        t.check(new ArrayList<>(s.spendByPlatform().keySet()).equals(platformOrder),
                "两张平台表的 key 顺序一致");

        List<Integer> years = new ArrayList<>(s.completedByYear().keySet());
        t.check(years.get(0) == 2023 && years.get(1) == 2024,
                "★ 年份升序（柱状图/报表按它逐行打印）");

        for (int i = 0; i < 5; i++) {
            Stats again = Stats.of(fixture());
            if (!new ArrayList<>(again.countByPlatform().keySet()).equals(platformOrder)) {
                t.check(false, "第 " + i + " 次计算的平台顺序变了");
                return;
            }
        }
        t.check(true, "重复计算 5 次，平台顺序稳定");
    }

    private static void testHidden(TestRunner t) {
        t.section("[33] Stats：排除隐藏项（§4.4）");

        GameLibrary lib = fixture();
        Game witcher = lib.find("The Witcher 3", Platform.GOG).orElseThrow();
        witcher.hide();

        Stats s = Stats.of(lib);

        t.check(s.totalGames() == 3, "★ 隐藏的那款不计入 totalGames");
        close(t, "★ 隐藏后总花费 368.0（少了 99）", s.totalSpent(), 368.0);
        t.check(!s.countByPlatform().containsKey(Platform.GOG),
                "★ 隐藏后 GOG 从平台分布里消失");
        t.check(!s.spendByPlatform().containsKey(Platform.GOG), "花费表同样消失");
        t.check(s.countByPlatform().get(Platform.STEAM) == 2, "Steam 仍是 2 款");

        t.check(lib.allIncludingHidden().size() == 4, "库里实际存了 4 条");
        t.check(s.totalGames() == lib.all().size(), "totalGames == lib.all().size()");
    }

    private static void testMostExpensivePlatform(TestRunner t) {
        t.section("[34] Stats：最烧钱的平台（§4.5）");

        checkPlatform(t, "Steam（¥368）最烧钱", Stats.of(fixture()).mostExpensivePlatform(),
                Platform.STEAM);

        GameLibrary allFree = new GameLibrary();
        allFree.add(game("GTA5", Platform.EPIC, 0.0, 8, null));
        Stats freeStats = Stats.of(allFree);
        t.check(freeStats.totalGames() == 1, "库里 1 款");
        t.check(freeStats.mostExpensivePlatform().isPresent(),
                "★ 只有 0 元游戏时，最烧钱的平台仍是 Epic（不能返回 empty）");
        checkPlatform(t, "★ 返回的是 EPIC", freeStats.mostExpensivePlatform(), Platform.EPIC);

        GameLibrary mixed = new GameLibrary();
        mixed.add(game("GTA5", Platform.EPIC, 0.0, 8, null));
        mixed.add(game("Elden Ring", Platform.STEAM, 298.0, 10, null));
        checkPlatform(t, "有一个非 0 时取非 0 的",
                Stats.of(mixed).mostExpensivePlatform(), Platform.STEAM);

        GameLibrary tie = new GameLibrary();
        tie.add(game("A", Platform.STEAM, 50.0, 5, null));
        tie.add(game("B", Platform.EPIC, 50.0, 5, null));
        checkPlatform(t, "并列时取声明顺序靠前的（STEAM 在 EPIC 之前）",
                Stats.of(tie).mostExpensivePlatform(), Platform.STEAM);

        GameLibrary tieZero = new GameLibrary();
        tieZero.add(game("A", Platform.STEAM, 0.0, 5, null));
        tieZero.add(game("B", Platform.EPIC, 0.0, 5, null));
        checkPlatform(t, "★ 并列且都是 0 时同样返回 STEAM",
                Stats.of(tieZero).mostExpensivePlatform(), Platform.STEAM);
    }

    private static void testRatingZero(TestRunner t) {
        t.section("[35] Stats：0 分是合法分数，参与平均");

        GameLibrary lib = new GameLibrary();
        lib.add(game("Good", Platform.STEAM, 10.0, 10, null));
        lib.add(game("Bad", Platform.STEAM, 10.0, 0, null));

        Stats s = Stats.of(lib);
        close(t, "★ (10 + 0) / 2 = 5.0，0 分被算进去", s.averageRating(), 5.0);
        t.check(s.countByPlatform().get(Platform.STEAM) == 2, "两款都在 Steam 组");

        GameLibrary single = new GameLibrary();
        single.add(game("OnlyZero", Platform.GOG, 0.0, 0, null));
        close(t, "只有一款 0 分游戏时平均分是 0.0", Stats.of(single).averageRating(), 0.0);
    }

    private static void close(TestRunner t, String what, double actual, double expected) {
        t.check(Math.abs(actual - expected) < 1e-9, what + "（实际 " + actual + "）");
    }

    private static void checkPlatform(TestRunner t, String what,
                                      Optional<Platform> actual, Platform expected) {
        if (actual.isEmpty()) {
            t.check(false, what + " -> 返回了 Optional.empty()");
        } else {
            t.check(actual.orElseThrow() == expected, what + " -> " + actual.orElseThrow());
        }
    }

    private static GameLibrary fixture() {
        GameLibrary lib = new GameLibrary();
        lib.add(game("Elden Ring", Platform.STEAM, 298.0, 10, LocalDate.of(2024, 3, 15)));
        lib.add(game("GTA5", Platform.EPIC, 0.0, 8, LocalDate.of(2024, 1, 1)));
        lib.add(game("Hades", Platform.STEAM, 70.0, 9, LocalDate.of(2023, 5, 1)));
        lib.add(game("The Witcher 3", Platform.GOG, 99.0, 9, null));
        return lib;
    }

    private static Game game(String title, Platform platform, double price, int rating,
                             LocalDate completedDate) {
        Game g = new Game(title, platform, "RPG", price,
                LocalDate.of(2020, 1, 1), rating, 10.0, "");
        if (completedDate != null) {
            g.markCompleted(completedDate);
        }
        return g;
    }
}

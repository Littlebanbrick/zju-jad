package gamelib.test;

import gamelib.model.Game;
import gamelib.model.Platform;
import gamelib.store.GameLibrary;
import gamelib.store.ImportReport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * GameLibrary 的测试：入库查重、Optional 契约、隐藏过滤、搜索、分组。
 *
 * <p>其中「未命中必须返回 Optional.empty() 而不是 null」是重点回归项 ——
 * 这类错误不给编译警告，只在调用方链式调用时炸 NPE。
 */
public final class GameLibraryTest {

    private GameLibraryTest() {
    }

    public static void run(TestRunner t) {
        testAddAndDuplicate(t);
        testFindAndContains(t);
        testRemove(t);
        testHidden(t);
        testSearch(t);
        testGroupAndGenres(t);
        testZjuScenarios(t);
        testAddAll(t);
    }

    // ---------- 入库与查重 ----------

    private static void testAddAndDuplicate(TestRunner t) {
        t.section("[12] 入库与查重（§4.1）");

        GameLibrary lib = new GameLibrary();
        t.check(lib.isEmpty(), "新库 isEmpty()");
        t.check(lib.size() == 0, "新库 size() == 0");
        t.check(lib.all().isEmpty(), "新库 all() 为空");

        lib.add(game("Elden Ring", Platform.STEAM, "ARPG", 298.0, 10, false));
        t.check(lib.size() == 1, "add 后 size() == 1");
        t.check(!lib.isEmpty(), "add 后 isEmpty() 为 false");

        t.expectInvalid("重复入库（标题+平台完全相同）",
                () -> lib.add(game("Elden Ring", Platform.STEAM, "ARPG", 298.0, 10, false)));
        t.check(lib.size() == 1, "重复被拒绝后库里仍是 1 条");

        t.expectInvalid("重复入库（标题大小写+空格不同）",
                () -> lib.add(game("  elden ring  ", Platform.STEAM, "ARPG", 1.0, 1, false)));
        t.check(lib.size() == 1, "大小写不同也算重复，仍是 1 条");

        lib.add(game("Elden Ring", Platform.EPIC, "ARPG", 0.0, 8, false));
        t.check(lib.size() == 2, "同标题不同平台 = 两款游戏（§2 的「各买了一次」）");

        lib.add(game("GTA5", Platform.EPIC, "Action", 0.0, 8, false));
        t.check(lib.size() == 3, "第三款入库成功");

        t.expectThrows(NullPointerException.class, "add(null)", () -> lib.add(null));
    }

    // ---------- find / contains ----------

    private static void testFindAndContains(TestRunner t) {
        t.section("[13] find / contains —— Optional 永不返回 null（§3）");

        GameLibrary lib = new GameLibrary();
        Game elden = game("Elden Ring", Platform.STEAM, "ARPG", 298.0, 10, false);
        lib.add(elden);

        Optional<Game> hit = lib.find("Elden Ring", Platform.STEAM);
        t.check(hit.isPresent(), "find 命中");
        t.check(hit.orElseThrow() == elden, "find 返回库里的同一个对象（引用相等）");
        t.check(lib.find("elden ring", Platform.STEAM).isPresent(), "find 大小写不敏感");
        t.check(lib.find("  ELDEN RING  ", Platform.STEAM).isPresent(), "find 容忍首尾空格");

        Optional<Game> miss = lib.find("Cyberpunk 2077", Platform.STEAM);
        t.check(miss != null, "★ 未命中时返回的是 Optional 对象，而不是 null");
        t.check(miss.isEmpty(), "未命中时 isEmpty() 为 true");
        t.check(lib.find("Elden Ring", Platform.GOG).isEmpty(), "同标题不同平台找不到");

        t.check(lib.find("Elden Ring", Platform.STEAM)
                        .map(Game::getTitle).orElse("?")
                        .equals("Elden Ring"),
                "map/orElse 链式读字段（这就是 Optional 的用法）");

        t.check(lib.contains("Elden Ring", Platform.STEAM), "contains 命中");
        t.check(!lib.contains("Elden Ring", Platform.GOG), "contains 未命中：平台不对");
        t.check(!lib.contains("Cyberpunk 2077", Platform.STEAM), "contains 未命中：标题不存在");
        t.check(lib.contains("elden ring", Platform.STEAM), "contains 大小写不敏感");
    }

    // ---------- remove ----------

    private static void testRemove(TestRunner t) {
        t.section("[14] 删除（§4.4）");

        GameLibrary lib = new GameLibrary();
        Game gta = game("GTA5", Platform.EPIC, "Action", 0.0, 8, false);
        lib.add(gta);

        Optional<Game> removed = lib.remove("gta5", Platform.EPIC);
        t.check(removed.isPresent(), "remove 返回被删对象");
        t.check(removed.orElseThrow() == gta, "返回的就是那个对象（UI 要拿它打印）");
        t.check(lib.size() == 0, "删后 size() == 0");
        t.check(lib.find("GTA5", Platform.EPIC).isEmpty(), "删后 find 为空");

        Optional<Game> missing = lib.remove("GTA5", Platform.EPIC);
        t.check(missing != null, "★ 删不存在的游戏时返回 Optional 而不是 null");
        t.check(missing.isEmpty(), "删不存在时 isEmpty() 为 true");
        t.check(lib.remove("从未存在", Platform.STEAM).isEmpty(), "删从未存在的游戏也安全");
    }

    // ---------- 隐藏 ----------

    private static void testHidden(TestRunner t) {
        t.section("[15] 隐藏过滤（§4.4 扩展）");

        GameLibrary lib = new GameLibrary();
        Game visible = game("Hades", Platform.STEAM, "Roguelike", 70.0, 9, true);
        Game witcher = game("Witcher 3", Platform.GOG, "RPG", 99.0, 9, true);
        lib.add(visible);
        lib.add(witcher);
        witcher.hide();

        t.check(lib.size() == 2, "size() 数的是总存储数（含隐藏）");
        t.check(lib.allIncludingHidden().size() == 2, "allIncludingHidden() 含隐藏");
        t.check(lib.all().size() == 1, "all() 排除隐藏");
        t.check(lib.all().get(0) == visible, "all() 留下的是未隐藏那个");

        t.check(lib.find("Witcher 3", Platform.GOG).isPresent(),
                "★ find 仍能找到隐藏的游戏（否则无法取消隐藏）");
        t.check(lib.contains("Witcher 3", Platform.GOG), "contains 也仍为 true");

        t.check(lib.filter(x -> true).size() == 1, "filter 排除隐藏");
        t.check(lib.searchByPlatform(Platform.GOG).isEmpty(), "按平台搜索排除隐藏");
        t.check(lib.searchByTitle("witcher").isEmpty(), "按标题搜索排除隐藏");
        t.check(!lib.allGenres().contains("RPG"), "allGenres() 排除隐藏游戏的类型");
        t.check(!lib.groupByPlatform().get(Platform.GOG).contains(witcher),
                "分组结果排除隐藏");

        witcher.unhide();
        t.check(lib.all().size() == 2, "unhide() 后它又回到 all() 里");
        witcher.hide();

        t.check(lib.remove("Witcher 3", Platform.GOG).isPresent(), "隐藏的游戏仍能被删除");
        t.check(lib.size() == 1, "删除隐藏项后 size() == 1");
    }

    // ---------- 搜索与筛选 ----------

    private static void testSearch(TestRunner t) {
        t.section("[16] 搜索与筛选（§2）");

        GameLibrary lib = fixture();

        t.check(lib.searchByTitle("witch").size() == 2,
                "★ §2：「witch」命中两款 The Witcher 3");
        t.check(lib.searchByTitle("WITCH").size() == 2, "搜索大小写不敏感");
        t.check(lib.searchByTitle("  witch  ").size() == 2, "搜索容忍首尾空格");
        t.check(lib.searchByTitle("elden").size() == 1, "「elden」命中 Elden Ring");
        t.check(lib.searchByTitle("zelda").isEmpty(), "无命中返回空 List（不是 null）");
        t.check(lib.searchByTitle("").size() == 5, "空关键词返回全部（contains(\"\") 恒真）");

        t.check(lib.searchByPlatform(Platform.STEAM).size() == 3, "Steam 上有 3 款");
        t.check(lib.searchByPlatform(Platform.GOG).size() == 1, "GOG 上有 1 款");
        t.check(lib.searchByPlatform(Platform.XBOX).isEmpty(), "Xbox 上没有游戏");

        t.check(lib.searchByGenre("RPG").size() == 3,
                "「RPG」命中 RPG 两款 + ARPG 一款（子串匹配）");
        t.check(lib.searchByGenre("roguelike").size() == 1, "类型搜索不区分大小写");
        t.check(lib.searchByGenre("ARPG").size() == 1, "「ARPG」只命中 ARPG");
        t.check(lib.searchByGenre("FPS").isEmpty(), "没买过 FPS -> 空");

        t.check(lib.searchByCompleted(true).size() == 3, "已通关 3 款");
        t.check(lib.searchByCompleted(false).size() == 2, "未通关 2 款");

        List<Game> freeGames = lib.searchByPriceRange(0, 0);
        t.check(freeGames.size() == 1, "★ §2：「0 元领的 Epic 游戏」→ 1 款");
        t.check(freeGames.get(0).getTitle().equals("GTA5"), "那一款是 GTA5");
        t.check(lib.searchByPriceRange(100, 200).size() == 1, "[100,200] 区间 1 款");
        t.check(lib.searchByPriceRange(0, 1000).size() == 5, "[0,1000] 覆盖全部");
        t.check(lib.searchByPriceRange(500, 1000).isEmpty(), "没有超过 ¥500 的游戏");

        t.check(lib.filter(g -> g.getPlatform() == Platform.STEAM && !g.isCompleted()).size() == 2,
                "复合查询：Steam 上未通关的有 2 款");

        List<Game> snapshot = lib.all();
        snapshot.clear();
        t.check(lib.size() == 5, "★ 改动返回的 List 不影响库（返回的是副本）");
    }

    // ---------- 分组与类型清单 ----------

    private static void testGroupAndGenres(TestRunner t) {
        t.section("[17] 分组与类型清单（§3 / §4.5）");

        GameLibrary lib = new GameLibrary();
        lib.add(game("Elden Ring", Platform.STEAM, "ARPG", 298.0, 10, false));
        lib.add(game("Hades", Platform.STEAM, "Roguelike", 70.0, 9, true));
        lib.add(game("GTA5", Platform.EPIC, "Action", 0.0, 8, true));

        Map<Platform, List<Game>> grouped = lib.groupByPlatform();
        t.check(grouped.size() == Platform.values().length,
                "分组含全部 " + Platform.values().length + " 个平台（含 0 款的）");
        t.check(grouped.get(Platform.STEAM).size() == 2, "Steam 组 2 款");
        t.check(grouped.get(Platform.EPIC).size() == 1, "Epic 组 1 款");
        t.check(grouped.get(Platform.XBOX) != null, "没游戏的平台也存在于 Map 中");
        t.check(grouped.get(Platform.XBOX).isEmpty(), "Xbox 组是空 List（不是 null）");
        t.check(grouped.get(Platform.GOG) != null, "GOG 组存在");

        t.check(new ArrayList<>(grouped.keySet()).get(0) == Platform.STEAM,
                "第一个 key 是 STEAM（Platform.values() 的声明顺序）");

        int total = 0;
        for (List<Game> list : grouped.values()) {
            total += list.size();
        }
        t.check(total == 3, "各组数量之和 == size()");

        // §4.5 的「各平台数量分布」是它的派生结果
        Map<String, Integer> counts = new LinkedHashMap<>();
        grouped.forEach((p, list) -> counts.put(p.toString(), list.size()));
        t.check(counts.get("Steam") == 2, "§4.5 数量分布：Steam = 2");
        t.check(counts.get("Epic") == 1, "§4.5 数量分布：Epic = 1");
        t.check(counts.get("Xbox") == 0, "§4.5 数量分布：Xbox = 0");

        Set<String> genres = lib.allGenres();
        t.check(genres.size() == 3, "3 个不同类型");
        t.check(genres.contains("ARPG") && genres.contains("Roguelike")
                && genres.contains("Action"), "类型清单内容正确");
        t.check(!genres.contains("RPG"), "没买 RPG 类型 -> 不在清单里");

        lib.add(game("Elden Ring 2", Platform.STEAM, "ARPG", 1.0, 1, false));
        t.check(lib.allGenres().size() == 3, "★ 又加一款 ARPG 后类型数仍是 3（去重）");
    }

    // ---------- §2 的五个真实场景 ----------

    private static void testZjuScenarios(TestRunner t) {
        t.section("[18] §2 场景化验证");

        GameLibrary lib = new GameLibrary();
        lib.add(game("GTA5", Platform.EPIC, "Action", 0.0, 8, true));
        lib.add(game("Elden Ring", Platform.STEAM, "OpenWorld", 298.0, 10, false));
        lib.add(game("Hades", Platform.STEAM, "Roguelike", 70.0, 9, true));
        lib.add(game("The Witcher 3", Platform.GOG, "RPG", 99.0, 9, false));
        lib.add(game("The Witcher 3", Platform.STEAM, "RPG", 158.0, 9, false));

        t.check(lib.contains("GTA5", Platform.EPIC), "场景 1：昨天 Epic 领的 GTA5 已入库");

        List<Game> unfinishedSteam = lib.filter(
                g -> g.getPlatform() == Platform.STEAM && !g.isCompleted());
        t.check(unfinishedSteam.size() == 2,
                "场景 2：Steam 上未通关的有 2 款（Elden Ring、The Witcher 3）");

        // §2 原话是「还没玩过的开放世界游戏」—— 三个条件复合。
        // 真实系统里「开放世界」通常是标签不是类型，这里简化成类型 OpenWorld。
        List<Game> openWorldUnfinished = lib.filter(g ->
                g.getPlatform() == Platform.STEAM
                        && !g.isCompleted()
                        && g.getGenre().equalsIgnoreCase("OpenWorld"));
        t.check(openWorldUnfinished.size() == 1
                        && openWorldUnfinished.get(0).getTitle().equals("Elden Ring"),
                "场景 2b：再加「开放世界」条件 -> 只剩 Elden Ring");

        double steamSpend = 0.0;
        for (Game g : lib.searchByPlatform(Platform.STEAM)) {
            steamSpend += g.getPurchasePrice();
        }
        t.check(steamSpend == 526.0,
                "场景 3：Steam 共花 ¥526（298 + 70 + 158）");

        List<Game> in2024 = lib.filter(
                g -> g.getCompletedDate() != null && g.getCompletedDate().getYear() == 2024);
        t.check(in2024.size() == 2, "场景 4：2024 年通关 2 款（GTA5、Hades）");

        t.check(lib.searchByTitle("witcher").size() == 2,
                "场景 5：巫师 3 已在 GOG 和 Steam 各买一次");
        t.expectInvalid("场景 5：再买一份 Steam 版被拒绝",
                () -> lib.add(game("The Witcher 3", Platform.STEAM, "RPG", 158.0, 9, false)));
        t.check(lib.find("The Witcher 3", Platform.EPIC).isEmpty(),
                "场景 5：Epic 版没买过，可以放心买");
    }

    // ---------- 批量导入（§4.1） ----------

    /**
     * {@code addAll} 与 {@code add} 的语义差别是这一组的核心：
     * {@code add} 遇到重复抛异常，{@code addAll} 跳过并计数 ——
     * 一行重复不该中断整批导入。
     */
    private static void testAddAll(TestRunner t) {
        t.section("[29] 批量导入 addAll（§4.1）");

        GameLibrary lib = new GameLibrary();

        ImportReport empty = lib.addAll(List.of());
        t.check(empty.added() == 0 && empty.duplicates() == 0 && empty.skipped() == 0,
                "空列表 -> 三个计数都是 0");
        t.check(empty.total() == 0, "total() == 0");
        t.check(lib.isEmpty(), "库仍是空的");

        ImportReport fresh = lib.addAll(List.of(
                game("Elden Ring", Platform.STEAM, "ARPG", 298.0, 10, false),
                game("GTA5", Platform.EPIC, "Action", 0.0, 8, false),
                game("Hades", Platform.STEAM, "Roguelike", 70.0, 9, false)));
        t.check(fresh.added() == 3, "3 条全部新增");
        t.check(fresh.duplicates() == 0, "没有重复");
        t.check(fresh.skipped() == 0, "没有跳过");
        t.check(fresh.total() == 3, "total() == 3");
        t.check(lib.size() == 3, "库里 3 条");

        // ★ 与 add 的关键差别
        ImportReport dup = lib.addAll(List.of(
                game("Elden Ring", Platform.STEAM, "ARPG", 298.0, 10, false),
                game("  elden ring  ", Platform.STEAM, "ARPG", 1.0, 1, false),
                game("Cyberpunk 2077", Platform.GOG, "RPG", 0.0, 7, false)));
        t.check(dup.added() == 1, "★ 只有 1 条新增");
        t.check(dup.duplicates() == 2, "★ 2 条重复被跳过，而不是中断整批导入");
        t.check(dup.duplicateTitles().size() == 2, "重复明细 2 条");
        t.check(dup.skipped() == 0, "重复不计入 skipped");
        t.check(lib.size() == 4, "库里 4 条");

        // 重复导入不能覆盖库里的原对象
        Game before = lib.find("Elden Ring", Platform.STEAM).orElseThrow();
        t.check(before.getPurchasePrice() == 298.0,
                "★ 重复导入不覆盖库里那份（价格仍是 298，不是 1.0）");
        t.check(before.getRating() == 10, "评分也没被那份 1 分的覆盖");

        // null：注意要用 Arrays.asList，List.of 不接受 null
        ImportReport withNull = lib.addAll(Arrays.asList(
                null, game("Stardew Valley", Platform.GOG, "Sim", 48.0, 9, false), null));
        t.check(withNull.added() == 1, "null 之外的那 1 条入库");
        t.check(withNull.skipped() == 2, "★ 2 个 null 计入 skipped");
        t.check(withNull.total() == 3, "total() 覆盖全部输入（含被跳过的）");
        t.check(lib.contains("Stardew Valley", Platform.GOG), "新入库的能查到");

        t.check(fresh.added() + fresh.duplicates() + fresh.skipped() == 3,
                "★ 不变量：added + duplicates + skipped == 输入条数");
        t.check(dup.added() + dup.duplicates() + dup.skipped() == 3,
                "★ 不变量在混合场景也成立");
        t.check(dup.duplicates() == dup.duplicateTitles().size(),
                "duplicates 计数与明细条数一致");

        // 顺序
        GameLibrary ordered = new GameLibrary();
        ordered.addAll(List.of(
                game("C", Platform.STEAM, "RPG", 1.0, 5, false),
                game("A", Platform.STEAM, "RPG", 1.0, 5, false),
                game("B", Platform.STEAM, "RPG", 1.0, 5, false)));
        List<Game> all = ordered.all();
        t.check(all.get(0).getTitle().equals("C")
                        && all.get(1).getTitle().equals("A")
                        && all.get(2).getTitle().equals("B"),
                "导入后保持输入顺序（LinkedHashMap）");

        // add 的语义没变，两个方法各司其职
        t.expectInvalid("add 仍然抛异常（只有 addAll 跳过重复）",
                () -> ordered.add(game("C", Platform.STEAM, "RPG", 1.0, 5, false)));

        t.check(lib.find("  stardew valley  ", Platform.GOG).isPresent(),
                "导入后 find 大小写不敏感");
    }

    // ---------- 测试夹具 ----------

    /**
     * 5 款游戏的固定库：
     * Witcher3(GOG,99,已通关) / Witcher3(Steam,158,未通关) / Elden Ring(Steam,298,未通关)
     * / GTA5(Epic,0,已通关) / Hades(Steam,70,已通关)
     */
    private static GameLibrary fixture() {
        GameLibrary lib = new GameLibrary();
        lib.add(game("The Witcher 3", Platform.GOG, "RPG", 99.0, 9, true));
        lib.add(game("The Witcher 3", Platform.STEAM, "RPG", 158.0, 9, false));
        lib.add(game("Elden Ring", Platform.STEAM, "ARPG", 298.0, 10, false));
        lib.add(game("GTA5", Platform.EPIC, "Action", 0.0, 8, true));
        lib.add(game("Hades", Platform.STEAM, "Roguelike", 70.0, 9, true));
        return lib;
    }

    /** 通关日期统一盖 2024-01-01，方便按年份筛选。 */
    private static Game game(String title, Platform platform, String genre,
                             double price, int rating, boolean completed) {
        Game g = new Game(title, platform, genre, price,
                LocalDate.of(2020, 1, 1), rating, 10.0, "");
        if (completed) {
            g.markCompleted(LocalDate.of(2024, 1, 1));
        }
        return g;
    }
}

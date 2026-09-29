package gamelib.test;

import gamelib.io.Csv;
import gamelib.io.GameCsv;
import gamelib.model.Game;
import gamelib.model.InvalidGameDataException;
import gamelib.model.Platform;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * GameCsv 的测试 —— <b>这份文件同时是 GameCsv 的接口规格</b>。
 *
 * <p>它要调用的 API（你先写空方法体让它能编译，再逐个填实现）：
 * <pre>
 * package gamelib.io;
 *
 * public final class GameCsv {
 *     public static final List&lt;String&gt; HEADER;   // 11 列，见下
 *
 *     public static List&lt;String&gt; toFields(Game g);
 *     public static Game fromFields(List&lt;String&gt; fields, int lineNumber);
 *     public static String toCsvText(List&lt;Game&gt; games);
 *     public static List&lt;Game&gt; parseCsvText(String text);
 * }
 * </pre>
 *
 * <p><b>11 列，顺序如下</b>（{@code HEADER} 必须恰好是这些字符串）：
 * <pre>
 * title, platform, genre, purchase_price, purchase_date,
 * completed, completed_date, playtime_hours, rating, note, hidden
 * </pre>
 *
 * <p><b>约定</b>：
 * <ol>
 *   <li>{@code platform} 写出用 {@code name()}（如 {@code STEAM}）；
 *       读入走 {@code Platform.fromString}，所以 {@code Steam} 也认</li>
 *   <li>{@code completed} 是<b>派生列</b>（{@code isCompleted() ? "y" : "n"}），
 *       <b>读入时忽略</b>；判定只以 {@code completed_date} 为准</li>
 *   <li>可空字段用空串表示：{@code completed_date} 为 null 时写 {@code ""}</li>
 *   <li>任何解析失败都抛 {@link InvalidGameDataException}，消息含
 *       {@code "第 N 行"}（N 是原始文本里的物理行号，第 1 行是表头）</li>
 *   <li>{@code toCsvText} 以 BOM 开头；{@code parseCsvText} 负责剥掉它</li>
 *   <li>{@code parseCsvText} 要求表头与 {@code HEADER} <b>逐位置完全一致</b>。
 *       顺序被调换时明确报错，不静默读错列（不迁就手工编辑过的文件）</li>
 * </ol>
 */
public final class GameCsvTest {

    private GameCsvTest() {
    }

    public static void run(TestRunner t) {
        testHeader(t);
        testToFields(t);
        testFromFields(t);
        testFromFieldsErrors(t);
        testDocumentRoundTrip(t);
        testParseRobustness(t);
    }

    // ---------- 表头 ----------

    private static void testHeader(TestRunner t) {
        t.section("[23] GameCsv.HEADER");

        t.check(GameCsv.HEADER.size() == 11, "11 列");
        t.check(GameCsv.HEADER.get(0).equals("title"), "[0] title");
        t.check(GameCsv.HEADER.get(1).equals("platform"), "[1] platform");
        t.check(GameCsv.HEADER.get(2).equals("genre"), "[2] genre");
        t.check(GameCsv.HEADER.get(3).equals("purchase_price"), "[3] purchase_price");
        t.check(GameCsv.HEADER.get(4).equals("purchase_date"), "[4] purchase_date");
        t.check(GameCsv.HEADER.get(5).equals("completed"), "[5] completed（派生列）");
        t.check(GameCsv.HEADER.get(6).equals("completed_date"), "[6] completed_date（真实来源）");
        t.check(GameCsv.HEADER.get(7).equals("playtime_hours"), "[7] playtime_hours");
        t.check(GameCsv.HEADER.get(8).equals("rating"), "[8] rating");
        t.check(GameCsv.HEADER.get(9).equals("note"), "[9] note");
        t.check(GameCsv.HEADER.get(10).equals("hidden"), "[10] hidden");
        t.check(GameCsv.HEADER.contains("cover_image_path") == false,
                "不含 cover_image_path（Game 里没有这个字段）");
    }

    // ---------- Game -> 字段 ----------

    private static void testToFields(TestRunner t) {
        t.section("[24] toFields：Game -> 11 个字段");

        Game done = new Game("Elden Ring", Platform.STEAM, "ARPG", 298.0,
                LocalDate.of(2022, 3, 1), 10, 120.0, "神作，DLC快出！");
        done.markCompleted(LocalDate.of(2024, 3, 15));

        List<String> f = GameCsv.toFields(done);
        t.check(f.size() == 11, "11 个字段");
        t.check(f.get(0).equals("Elden Ring"), "[0] title");
        t.check(f.get(1).equals("STEAM"), "★ [1] platform 用 name() 而不是 label");
        t.check(f.get(2).equals("ARPG"), "[2] genre");
        t.check(f.get(3).equals("298.0"), "[3] purchase_price");
        t.check(f.get(4).equals("2022-03-01"), "★ [4] purchase_date 是 ISO 格式");
        t.check(f.get(5).equals("y"), "[5] completed = y");
        t.check(f.get(6).equals("2024-03-15"), "[6] completed_date");
        t.check(f.get(7).equals("120.0"), "[7] playtime_hours");
        t.check(f.get(8).equals("10"), "[8] rating");
        t.check(f.get(9).equals("神作，DLC快出！"), "[9] note");
        t.check(f.get(10).equals("false"), "[10] hidden = false");

        Game fresh = new Game("GTA5", Platform.EPIC, "Action", 0.0,
                LocalDate.of(2020, 5, 14), 8, 25.0, null);
        List<String> f2 = GameCsv.toFields(fresh);
        t.check(f2.get(5).equals("n"), "未通关 -> completed = n");
        t.check(f2.get(6).isEmpty(),
                "★ 未通关 -> completed_date 是空串（不是 null，也不是字面量 \"null\"）");
        t.check(f2.get(3).equals("0.0"), "价格 0 -> \"0.0\"");
        t.check(f2.get(9).isEmpty(), "note 从不为 null -> 空串");
        t.check(f2.get(10).equals("false"), "默认不隐藏");

        fresh.hide();
        t.check(GameCsv.toFields(fresh).get(10).equals("true"), "隐藏后 hidden = true");

        // 刁钻内容必须靠 Csv 转义，toFields 本身不做转义
        Game nasty = new Game("A", Platform.GOG, "RPG", 1.0, LocalDate.of(2020, 1, 1),
                5, 1.0, "神作\", DLC别买");
        t.check(GameCsv.toFields(nasty).get(9).equals("神作\", DLC别买"),
                "toFields 返回原始值，转义交给 Csv.formatRecord");
    }

    // ---------- 字段 -> Game ----------

    private static void testFromFields(TestRunner t) {
        t.section("[25] fromFields：字段 -> Game（方案 B 的补盖章路径）");

        Game g = GameCsv.fromFields(List.of(
                "Elden Ring", "STEAM", "ARPG", "298.0", "2022-03-01",
                "y", "2024-03-15", "120.0", "10", "神作", "false"), 2);

        t.check(g.getTitle().equals("Elden Ring"), "title");
        t.check(g.getPlatform() == Platform.STEAM, "platform");
        t.check(g.getGenre().equals("ARPG"), "genre");
        t.check(g.getPurchasePrice() == 298.0, "purchasePrice");
        t.check(g.getPurchaseDate().equals(LocalDate.of(2022, 3, 1)), "purchaseDate");
        t.check(g.getPlaytimeHours() == 120.0, "playtimeHours");
        t.check(g.getRating() == 10, "rating");
        t.check(g.getNotes().equals("神作"), "notes");
        t.check(g.isCompleted(), "isCompleted 由 completed_date 决定");
        t.check(g.getCompletedDate().equals(LocalDate.of(2024, 3, 15)), "completedDate 完整");
        t.check(!g.isHidden(), "hidden = false");

        // platform 列接受 label 形式（人手改过 Excel 的情况）
        Game labeled = GameCsv.fromFields(List.of(
                "A", "Steam", "RPG", "1.0", "2020-01-01",
                "n", "", "1.0", "5", "", "false"), 3);
        t.check(labeled.getPlatform() == Platform.STEAM,
                "★ platform 列写 \"Steam\"（label）也能读");

        // 未通关：completed_date 为空
        t.check(!labeled.isCompleted(), "completed_date 空 -> 未通关");
        t.check(labeled.getCompletedDate() == null, "completedDate 是 null 而不是空 LocalDate");

        // ★ 方案 B 的核心：completed 列说 y 但日期为空 -> 以日期为准
        Game liar = GameCsv.fromFields(List.of(
                "A", "STEAM", "RPG", "1.0", "2020-01-01",
                "y", "", "1.0", "5", "", "false"), 4);
        t.check(!liar.isCompleted(),
                "★ completed 列说 y 但日期为空 -> 仍视为未通关（第 6 列读入时忽略）");

        // 反过来：completed 列说 n 但有日期 -> 也算已通关
        Game reverse = GameCsv.fromFields(List.of(
                "B", "STEAM", "RPG", "1.0", "2020-01-01",
                "n", "2024-06-01", "1.0", "5", "", "false"), 5);
        t.check(reverse.isCompleted(), "★ completed 列说 n 但有日期 -> 已通关");

        // hidden
        Game hiddenGame = GameCsv.fromFields(List.of(
                "C", "GOG", "RPG", "1.0", "2020-01-01",
                "n", "", "1.0", "5", "", "true"), 6);
        t.check(hiddenGame.isHidden(), "hidden = true -> 已隐藏");

        // 空笔记
        t.check(labeled.getNotes().isEmpty(), "note 空串 -> 空串（不是 null）");
    }

    // ---------- 错误报告 ----------

    private static void testFromFieldsErrors(TestRunner t) {
        t.section("[26] fromFields 的错误报告：必须带行号");

        expectInvalidWith(t, "价格不是数字", 7, "purchase_price", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "abc", "2020-01-01",
                        "n", "", "1.0", "5", "", "false"), 7));

        // 下面三条是 Game 的「领域规则」（价格非负、标题非空、日期非未来），不是 CSV 的
        // 「格式问题」—— 没有哪一列单独负责，所以只断言行号，不强求列名（也不强求措辞）。
        // 列名只对单列转换失败（如 "abc" 解析成数字）才有意义。
        expectInvalidAt(t, "价格是负数", 7, () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "-1.0", "2020-01-01",
                        "n", "", "1.0", "5", "", "false"), 7));

        expectInvalidWith(t, "日期格式错（13 月）", 8, "purchase_date", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", "2022-13-01",
                        "n", "", "1.0", "5", "", "false"), 8));

        expectInvalidWith(t, "日期不是 ISO（斜杠分隔）", 8, "purchase_date", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", "2022/03/01",
                        "n", "", "1.0", "5", "", "false"), 8));

        expectInvalidWith(t, "评分越界 11", 9, "个人评分", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", "2020-01-01",
                        "n", "", "1.0", "11", "", "false"), 9));

        expectInvalidWith(t, "评分不是整数", 9, "rating", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", "2020-01-01",
                        "n", "", "1.0", "八分", "", "false"), 9));

        expectInvalidWith(t, "时长不是数字", 10, "playtime_hours", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", "2020-01-01",
                        "n", "", "many", "5", "", "false"), 10));

        expectInvalidWith(t, "未知平台", 11, "platform", () ->
                GameCsv.fromFields(List.of(
                        "A", "Nintendo", "RPG", "1.0", "2020-01-01",
                        "n", "", "1.0", "5", "", "false"), 11));

        expectInvalidWith(t, "hidden 不是 y/n", 12, "hidden", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", "2020-01-01",
                        "n", "", "1.0", "5", "", "maybe"), 12));

        expectInvalidAt(t, "标题为空", 13, () ->
                GameCsv.fromFields(List.of(
                        "   ", "STEAM", "RPG", "1.0", "2020-01-01",
                        "n", "", "1.0", "5", "", "false"), 13));

        expectInvalidAt(t, "未来购买日期", 14, () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", LocalDate.now().plusDays(1).toString(),
                        "n", "", "1.0", "5", "", "false"), 14));

        // Game.markCompleted 的一致性检查也要带上行号
        expectInvalidWith(t, "通关日期早于购买日期", 15, "通关日期", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", "2020-01-01",
                        "y", "2019-01-01", "1.0", "5", "", "false"), 15));

        // 列数不对通常意味着文件被手工截断
        expectInvalidWith(t, "字段太少", 16, "11", () ->
                GameCsv.fromFields(List.of("A", "STEAM", "RPG"), 16));
        expectInvalidWith(t, "字段太多", 17, "11", () ->
                GameCsv.fromFields(List.of(
                        "A", "STEAM", "RPG", "1.0", "2020-01-01",
                        "n", "", "1.0", "5", "", "false", "多出来的"), 17));
    }

    // ---------- 文档级 round-trip ----------

    private static void testDocumentRoundTrip(TestRunner t) {
        t.section("[27] toCsvText / parseCsvText：文档级 round-trip");

        List<Game> games = sampleGames();
        String text = GameCsv.toCsvText(games);

        t.check(text.startsWith(Csv.BOM), "★ 文档以 BOM 开头（Excel 打开中文不乱码）");
        t.check(text.contains("title,platform,genre,purchase_price,purchase_date"),
                "含表头行");
        t.check(text.endsWith("\n") || text.endsWith("\r\n"), "以换行结尾");
        t.check(!text.contains("\r\n\r\n"), "没有多余空行");

        List<Game> back = GameCsv.parseCsvText(text);
        t.check(back.size() == games.size(),
                "条数一致：" + games.size() + " -> " + back.size());

        boolean allSame = true;
        String firstDiff = "";
        for (int i = 0; i < Math.min(games.size(), back.size()); i++) {
            if (!dump(games.get(i)).equals(dump(back.get(i)))) {
                allSame = false;
                if (firstDiff.isEmpty()) {
                    firstDiff = "\n      原: " + dump(games.get(i))
                              + "\n      回: " + dump(back.get(i));
                }
            }
        }
        // Game.equals 只比 key，所以这里逐字段 dump 对比，才能抓出"字段丢了但 key 对"的情况
        t.check(allSame, "★ round-trip：全部字段逐一还原（不是只比 key）" + firstDiff);

        t.check(back.get(0).isCompleted()
                        && back.get(0).getCompletedDate().equals(LocalDate.of(2024, 3, 15)),
                "已通关记录的日期完整回来");
        t.check(!back.get(1).isCompleted() && back.get(1).getCompletedDate() == null,
                "未通关记录回来仍是未通关");
        t.check(back.get(2).isHidden(), "隐藏标记回来");
        t.check(back.get(3).getNotes().equals("神作\", DLC别买"),
                "★ 带引号和逗号的笔记完整回来");
        t.check(back.get(4).getNotes().isEmpty(), "空笔记回来仍是空串");
        t.check(back.get(5).getPurchasePrice() == 0.0, "0 元记录的价格回来");

        // 空库
        String emptyText = GameCsv.toCsvText(List.of());
        t.check(emptyText.startsWith(Csv.BOM), "空库也有 BOM");
        t.check(GameCsv.parseCsvText(emptyText).isEmpty(), "空库 round-trip 得到空列表");

        // 二次 round-trip 必须稳定（幂等）
        String text2 = GameCsv.toCsvText(back);
        t.check(text2.equals(text), "★ 二次导出与首次逐字节相同（幂等）");
    }

    // ---------- 读入的健壮性 ----------

    private static void testParseRobustness(TestRunner t) {
        t.section("[28] parseCsvText 的健壮性");

        // 列顺序调换：本项目要求严格按 HEADER 顺序，顺序错就报错，不迁就手工编辑
        String swapped = "note,title,platform,genre,purchase_price,purchase_date,"
                + "completed,completed_date,playtime_hours,rating,hidden\n"
                + "我的笔记,Elden Ring,Steam,ARPG,298.0,2022-03-01,y,2024-03-15,120.0,10,false";
        expectInvalidWith(t, "★ 列顺序调换 -> 明确报「顺序错误」，而不是静默读错列",
                1, "顺序", () -> GameCsv.parseCsvText(swapped));

        // 没有 BOM 也能读
        String noBom = Csv.formatRecord(GameCsv.HEADER) + "\n"
                + "A,STEAM,RPG,1.0,2020-01-01,n,,1.0,5,,false";
        t.check(GameCsv.parseCsvText(noBom).size() == 1, "★ 没有 BOM 的文件也能读");

        // 空行跳过
        String withBlanks = Csv.formatRecord(GameCsv.HEADER) + "\n"
                + "\n"
                + "A,STEAM,RPG,1.0,2020-01-01,n,,1.0,5,,false\n"
                + "\n";
        t.check(GameCsv.parseCsvText(withBlanks).size() == 1, "★ 空行被跳过");

        // 只有表头
        t.check(GameCsv.parseCsvText(Csv.formatRecord(GameCsv.HEADER)).isEmpty(),
                "只有表头 -> 空列表");
        t.check(GameCsv.parseCsvText("").isEmpty(), "空文本 -> 空列表");

        // 行号是物理行号：表头在第 1 行，空行也计数
        String badAtLine4 = Csv.formatRecord(GameCsv.HEADER) + "\n"
                + "A,STEAM,RPG,1.0,2020-01-01,n,,1.0,5,,false\n"
                + "\n"
                + "B,STEAM,RPG,notanumber,2020-01-01,n,,1.0,5,,false\n";
        expectInvalidWith(t, "★ 行号是物理行号（含空行）", 4, "purchase_price",
                () -> GameCsv.parseCsvText(badAtLine4));

        // 缺列
        expectInvalidWith(t, "★ 缺列必须明确报错", 1, "purchase_price", () ->
                GameCsv.parseCsvText("title,platform,genre\nElden Ring,Steam,ARPG"));

        // 首行不是表头
        expectInvalidWith(t, "首行不是表头", 1, "表头", () ->
                GameCsv.parseCsvText("Elden Ring,Steam,ARPG,298,2022-03-01,y,,120,10,,false"));
    }

    // ---------- 工具 ----------

    /**
     * 逐字段快照。不能只靠 {@code Game.equals} —— 它只比 key，
     * 字段全丢了也会判相等。
     */
    private static String dump(Game g) {
        return g.key()
                + "|genre=" + g.getGenre()
                + "|price=" + g.getPurchasePrice()
                + "|bought=" + g.getPurchaseDate()
                + "|completed=" + g.isCompleted()
                + "|completedOn=" + g.getCompletedDate()
                + "|hours=" + g.getPlaytimeHours()
                + "|rating=" + g.getRating()
                + "|note=" + g.getNotes()
                + "|hidden=" + g.isHidden();
    }

    /**
     * 只断言行号，不管措辞和列名。
     *
     * <p>用于 Game 的领域规则错误（价格非负、标题非空、日期非未来）：
     * 这些规则由 {@code Game} 守，一次构造失败无法归到某一列，
     * 所以列名不作为要求 —— 否则测试会变成在锁定实现的消息格式。
     */
    private static void expectInvalidAt(TestRunner t, String what, int line, Runnable action) {
        try {
            action.run();
            t.check(false, what + " —— 应当抛异常，但没有");
        } catch (InvalidGameDataException e) {
            String msg = String.valueOf(e.getMessage());
            t.check(msg.contains("第 " + line + " 行"), what + " -> " + msg);
        } catch (Throwable e) {
            t.check(false, what + " —— 抛了非预期异常："
                    + e.getClass().getSimpleName() + "：" + e.getMessage());
        }
    }

    /** 断言抛 InvalidGameDataException，且消息里同时含行号和某个关键词。 */
    private static void expectInvalidWith(TestRunner t, String what, int line,
                                          String mustContain, Runnable action) {        try {
            action.run();
            t.check(false, what + " —— 应当抛异常，但没有");
        } catch (InvalidGameDataException e) {
            String msg = String.valueOf(e.getMessage());
            boolean ok = msg.contains("第 " + line + " 行") && msg.contains(mustContain);
            t.check(ok, what + " -> " + msg);
        } catch (Throwable e) {
            t.check(false, what + " —— 抛了非预期异常："
                    + e.getClass().getSimpleName() + "：" + e.getMessage());
        }
    }

    /** 6 款游戏，覆盖通关闭环、隐藏、空笔记、带引号逗号的笔记、0 元。 */
    private static List<Game> sampleGames() {
        List<Game> games = new ArrayList<>();

        Game a = new Game("Elden Ring", Platform.STEAM, "ARPG", 298.0,
                LocalDate.of(2022, 3, 1), 10, 120.0, "神作，DLC快出！");
        a.markCompleted(LocalDate.of(2024, 3, 15));
        games.add(a);

        games.add(new Game("GTA5", Platform.EPIC, "Action", 0.0,
                LocalDate.of(2020, 5, 14), 8, 25.0, "Epic白嫖"));

        Game hidden = new Game("The Witcher 3", Platform.GOG, "RPG", 99.0,
                LocalDate.of(2018, 6, 1), 9, 60.0, "剧情神作");
        hidden.hide();
        games.add(hidden);

        games.add(new Game("Hollow Knight", Platform.STEAM, "Metroidvania", 50.0,
                LocalDate.of(2021, 5, 1), 9, 30.0, "神作\", DLC别买"));

        Game noNote = new Game("Hades", Platform.STEAM, "Roguelike", 70.0,
                LocalDate.of(2021, 1, 1), 9, 40.0, null);
        noNote.markCompleted(LocalDate.of(2024, 1, 20));
        games.add(noNote);

        games.add(new Game("Cyberpunk 2077", Platform.GOG, "RPG", 0.0,
                LocalDate.of(2023, 11, 1), 7, 15.5, "等 DLC"));

        return games;
    }
}

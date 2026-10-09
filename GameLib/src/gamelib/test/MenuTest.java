package gamelib.test;

import gamelib.io.GameCsv;
import gamelib.io.LibraryFiles;
import gamelib.io.LoadReport;
import gamelib.model.Game;
import gamelib.model.Platform;
import gamelib.store.GameLibrary;
import gamelib.ui.Console;
import gamelib.ui.Menu;
import gamelib.ui.Stats;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public final class MenuTest {

    private MenuTest() {
    }

    public static void run(TestRunner t) {
        testStartupAndExit(t);
        testAdd(t);
        testListAndSearch(t);
        testUpdate(t);
        testDeleteAndHide(t);
        testStats(t);
        testPersistence(t);
        testSaveFailureKeepsRunning(t);
        testBatchImport(t);
    }

    private static void testStartupAndExit(TestRunner t) {
        t.section("[51] Menu：启动提示与退出");

        Fixture f = fixture(new GameLibrary(), "");
        f.run();

        t.check(f.output().contains("Personal Game Library Manager"), "打印了标题");
        t.check(f.output().contains("正在加载游戏库"), "打印了加载提示");
        t.check(f.output().contains("未找到任何存档"), "空目录 -> 提示新建游戏库");
        t.check(f.output().contains("主菜单"), "打印了主菜单");
        t.check(f.output().contains("感谢使用"), "★ 空脚本（EOF）也能正常退出，不死循环");

        t.check(!f.output().contains("已从 CSV 加载"), "★ 启动提示不重复（结果只在 warning 里）");
    }

    private static void testAdd(TestRunner t) {
        t.section("[52] Menu：添加游戏（§4.1）");

        // 1 -> 添加游戏，1 -> 手动录入
        String script = "1\n1\nSteam\nElden Ring\nARPG\n298\n2022-03-01\ny\n120\n10\n神作，DLC快出！\n";
        Fixture f = fixture(new GameLibrary(), script);
        f.run();

        t.check(f.output().contains("已成功入库：Elden Ring | Steam"), "提示了入库成功");
        t.check(f.lib.size() == 1, "库里 1 条");

        Game g = f.lib.find("Elden Ring", Platform.STEAM).orElseThrow();
        t.check(g.getPurchasePrice() == 298.0, "价格 298");
        t.check(g.getRating() == 10, "评分 10");
        t.check(g.getPlaytimeHours() == 120.0, "时长 120");
        t.check(g.getNotes().equals("神作，DLC快出！"), "中文笔记");
        t.check(g.isCompleted(), "★ 回答 y -> 已通关（盖了日期）");
        t.check(g.getCompletedDate() != null, "completedDate 非 null");

        Fixture no = fixture(new GameLibrary(),
                "1\n1\nEpic\nGTA5\nAction\n0\n2020-05-14\nn\n25\n8\n\n");
        no.run();
        Game gta = no.lib.find("GTA5", Platform.EPIC).orElseThrow();
        t.check(!gta.isCompleted(), "★ 回答 n -> 未通关");
        t.check(gta.getCompletedDate() == null, "★ completedDate 保持 null（不能填 now() 冒充）");
        t.check(gta.getNotes().isEmpty(), "笔记回车 -> 空串");

        Fixture dup = fixture(new GameLibrary(),
                "1\n1\nSteam\nElden Ring\nARPG\n298\n2022-03-01\nn\n120\n10\n\n"
                        + "1\n1\nSteam\nElden Ring\nARPG\n1\n2022-03-01\nn\n1\n1\n\n");
        dup.run();
        t.check(dup.output().contains("已存在"), "★ 重复入库给出提示");
        t.check(dup.lib.size() == 1, "★ 重复后库里仍只有 1 条");
        t.check(dup.output().contains("主菜单"), "★ 重复是警告不是崩溃，回到了菜单");

        Fixture bad = fixture(new GameLibrary(),
                "1\n1\nSteam\nHades\nRoguelike\nabc\n70\n2021-01-01\nn\n-5\n40\n99\n9\n\n");
        bad.run();
        t.check(bad.output().contains("【警告】"), "★ 非法输入打出了警告");
        t.check(bad.lib.size() == 1, "★ 重问后仍然入库成功");
        Game hades = bad.lib.find("Hades", Platform.STEAM).orElseThrow();
        t.check(hades.getPurchasePrice() == 70.0, "价格最终是 70");
        t.check(hades.getRating() == 9, "评分最终是 9（99 被拒后重问）");

        Fixture cancel = fixture(new GameLibrary(), "1\n1\n\n");
        cancel.run();
        t.check(cancel.output().contains("已取消"), "★ 必填字段留空 -> 已取消");
        t.check(cancel.lib.isEmpty(), "库仍然是空的");
        t.check(cancel.output().contains("主菜单"), "取消后回到菜单");
    }

    private static void testListAndSearch(TestRunner t) {
        t.section("[53] Menu：查看全部与搜索（§2）");

        GameLibrary lib = library(
                game("The Witcher 3", Platform.GOG, 99.0, 9),
                game("Elden Ring", Platform.STEAM, 298.0, 10));

        Fixture list = fixture(lib, "2\n");
        list.run();
        t.check(list.output().contains("共 2 款游戏"), "打印了总数");
        t.check(list.output().contains("The Witcher 3"), "含第 1 款");
        t.check(list.output().contains("Elden Ring"), "含第 2 款");

        Fixture quick = fixture(library(
                game("The Witcher 3", Platform.GOG, 99.0, 9),
                game("Elden Ring", Platform.STEAM, 298.0, 10)), "search witcher\n");
        quick.run();
        t.check(quick.output().contains("搜索结果（1 条"), "★ 快速命令 search 命中 1 条");
        t.check(quick.output().contains("The Witcher 3"), "命中巫师 3");

        Fixture miss = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)), "search zelda\n");
        miss.run();
        t.check(miss.output().contains("未找到匹配的游戏"), "无命中给提示而不是空刷屏");

        Fixture viaMenu = fixture(library(
                game("The Witcher 3", Platform.GOG, 99.0, 9)), "3\n1\nwitcher\n");
        viaMenu.run();
        t.check(viaMenu.output().contains("搜索结果"), "★ 走编号菜单也能搜到");

        Fixture empty = fixture(new GameLibrary(), "2\n");
        empty.run();
        t.check(empty.output().contains("库里还没有游戏"), "空库给提示");
    }

    private static void testUpdate(TestRunner t) {
        t.section("[54] Menu：更新游戏（§4.3）");

        Fixture hours = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nGTA5|Epic\n1\n5\n0\n");
        hours.run();
        Game gta = hours.lib.find("GTA5", Platform.EPIC).orElseThrow();
        t.check(gta.getPlaytimeHours() == 30.0, "★ 25h + 5h = 30h");
        t.check(hours.output().contains("更新后游玩时长"), "打印了新时长");

        Fixture done = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nGTA5|Epic\n2\n\n0\n");
        done.run();
        t.check(done.lib.find("GTA5", Platform.EPIC).orElseThrow().isCompleted(),
                "★ 子菜单 2 标记通关");
        t.check(done.lib.find("GTA5", Platform.EPIC).orElseThrow()
                        .getCompletedDate().equals(LocalDate.now()),
                "★ 回车 -> 通关日期是今天");
        t.check(done.output().contains("已标记通关"), "给了提示");

        Fixture backfill = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nGTA5|Epic\n2\n2024-03-15\n0\n");
        backfill.run();
        Game backfilled = backfill.lib.find("GTA5", Platform.EPIC).orElseThrow();
        t.check(backfilled.getCompletedDate().equals(LocalDate.of(2024, 3, 15)),
                "★ 可补录历史通关日期（否则年度统计永远是今年）");
        t.check(backfilled.getCompletedDate().getYear() == 2024, "★ 年份是 2024");
        t.check(Stats.of(backfill.lib).completedByYear().get(2024) == 1,
                "★★ 年度通关统计能对上 2024");

        Fixture redo = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nGTA5|Epic\n2\n2024-03-15\n2\n2023-01-20\n0\n");
        redo.run();
        t.check(redo.lib.find("GTA5", Platform.EPIC).orElseThrow()
                        .getCompletedDate().equals(LocalDate.of(2023, 1, 20)),
                "★ 再标记会覆盖原日期");
        t.check(redo.output().contains("通关日期已改为"), "★ 覆盖时明说了是「改为」");

        Fixture tooEarly = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nGTA5|Epic\n2\n2019-01-01\n2\n2024-03-15\n0\n");
        tooEarly.run();
        t.check(tooEarly.output().contains("不能早于购买日期"), "★ 早于购买日期时警告");
        t.check(tooEarly.output().contains("当前："), "★ 仍在更新子菜单里（没被弹回主菜单）");
        t.check(tooEarly.lib.find("GTA5", Platform.EPIC).orElseThrow()
                        .getCompletedDate().equals(LocalDate.of(2024, 3, 15)),
                "★ 接下来那次合法输入生效了");

        Fixture rate = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nGTA5|Epic\n3\n10\n0\n");
        rate.run();
        t.check(rate.lib.find("GTA5", Platform.EPIC).orElseThrow().getRating() == 10,
                "★ 子菜单 3 更新评分");

        Fixture note = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nGTA5|Epic\n4\n剧情神作, DLC别买\n0\n");
        note.run();
        t.check(note.lib.find("GTA5", Platform.EPIC).orElseThrow()
                        .getNotes().equals("剧情神作, DLC别买"),
                "★ 子菜单 4 记录笔记（含半角逗号）");

        Game withNote = game("GTA5", Platform.EPIC, 0.0, 8);
        withNote.setNotes("原有的笔记");
        Fixture keep = fixture(library(withNote), "4\nGTA5|Epic\n4\n\n0\n");
        keep.run();
        t.check(keep.lib.find("GTA5", Platform.EPIC).orElseThrow()
                        .getNotes().equals("原有的笔记"),
                "★ 笔记回车保持不变");

        Fixture missing = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nElden Ring|Steam\nGTA5|Epic\n0\n");
        missing.run();
        t.check(missing.output().contains("没找到"), "★ 找不到时提示并重问");

        Fixture format = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "4\nGTA5\nGTA5|Epic\n0\n");
        format.run();
        t.check(format.output().contains("格式应为"), "★ 缺「|平台」时提示格式");
    }

    private static void testDeleteAndHide(TestRunner t) {
        t.section("[55] Menu：删除与隐藏（§4.4）");

        Fixture yes = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "5\nGTA5|Epic\n1\ny\n");
        yes.run();
        t.check(yes.output().contains("已删除：GTA5 | Epic"), "★ 确认后删除");
        t.check(yes.lib.isEmpty(), "库里空了");
        t.check(yes.output().contains("确认永久删除"), "★ 删之前问了确认（§4.4）");

        Fixture no = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "5\nGTA5|Epic\n1\n\n");
        no.run();
        t.check(no.output().contains("已放弃删除"), "★ 空回车 -> 放弃删除");
        t.check(no.lib.size() == 1, "★ 库里那条还在");

        Fixture hide = fixture(library(
                game("GTA5", Platform.EPIC, 0.0, 8),
                game("Hades", Platform.STEAM, 70.0, 9)), "5\nGTA5|Epic\n2\n");
        hide.run();
        t.check(hide.output().contains("已隐藏"), "隐藏成功");
        t.check(hide.lib.size() == 2, "★ size() 不变（含隐藏）");
        t.check(hide.lib.all().size() == 1, "★ all() 少 1");
        t.check(hide.lib.find("GTA5", Platform.EPIC).orElseThrow().isHidden(), "那条已隐藏");

        Fixture listHidden = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "5\nGTA5|Epic\n2\n2\n");
        listHidden.run();
        t.check(listHidden.output().contains("没有可显示的游戏"), "★ 全隐藏时查看全部给提示");

        Fixture delHidden = fixture(library(game("GTA5", Platform.EPIC, 0.0, 8)),
                "5\nGTA5|Epic\n2\n5\nGTA5|Epic\n1\ny\n");
        delHidden.run();
        t.check(delHidden.output().contains("已隐藏"), "先隐藏");
        t.check(delHidden.output().contains("已删除"), "★ 隐藏项仍能找到并永久删除");
        t.check(delHidden.lib.isEmpty(), "库里空了");
    }

    private static void testStats(TestRunner t) {
        t.section("[56] Menu：统计报表（§4.5）");

        Fixture f = fixture(library(
                game("Elden Ring", Platform.STEAM, 298.0, 10),
                game("Hades", Platform.STEAM, 70.0, 9),
                game("GTA5", Platform.EPIC, 0.0, 8)), "6\n");
        f.run();

        String out = f.output();
        t.check(out.contains("个人游戏资产"), "报表标题");
        t.check(out.contains("总游戏数") && out.contains("3"), "总游戏数 3");
        t.check(out.contains("¥368.00"), "★ 总花费 ¥368.00");
        t.check(out.contains("9.0"), "平均评分 9.0");
        t.check(out.contains("各平台数量"), "平台分布标题");
        t.check(out.contains("█"), "★ 画出了柱状图");
        t.check(out.contains("Steam") && out.contains("Epic"), "含两个平台");
        t.check(out.contains("最烧钱的平台"), "最烧钱的平台一行");
        t.check(out.contains("已通关") && out.contains("未通关"), "通关/未通关计数");

        Fixture empty = fixture(new GameLibrary(), "6\n");
        empty.run();
        t.check(empty.output().contains("没有可统计的数据"), "空库给提示不刷屏");
    }

    private static void testPersistence(TestRunner t) {
        t.section("[57] Menu：保存与加载（§4.6）");

        Fixture bin = fixture(library(
                game("Elden Ring", Platform.STEAM, 298.0, 10),
                game("GTA5", Platform.EPIC, 0.0, 8)), "7\n1\n");
        bin.run();
        t.check(bin.output().contains("完成！"), "提示保存完成");
        t.check(Files.exists(bin.files.binaryPath()), "★ 生成了 game_lib.dat");

        LoadReport back = bin.files.load();
        t.check(back.gameCount() == 2, "★ 读回 2 条");
        t.check(back.source() == LoadReport.Source.BINARY, "来源是 BINARY");

        Fixture csv = fixture(library(game("Hades", Platform.STEAM, 70.0, 9)), "7\n2\n");
        csv.run();
        t.check(Files.exists(csv.files.csvPath()), "★ 生成了 game_lib.csv");
        try {
            String text = Files.readString(csv.files.csvPath(), StandardCharsets.UTF_8);
            t.check(text.startsWith("\uFEFF"), "CSV 以 BOM 开头（Excel 中文不乱码）");
            t.check(text.contains("Hades,STEAM"), "CSV 含数据行");
        } catch (IOException e) {
            t.check(false, "读 CSV 失败：" + e);
        }

        GameLibrary withHidden = library(
                game("Elden Ring", Platform.STEAM, 298.0, 10),
                game("GTA5", Platform.EPIC, 0.0, 8));
        withHidden.find("GTA5", Platform.EPIC).orElseThrow().hide();

        Fixture hiddenBin = fixture(withHidden, "7\n1\n");
        hiddenBin.run();
        LoadReport reloaded = hiddenBin.files.load();
        t.check(reloaded.gameCount() == 2, "★ 隐藏的游戏也存进了磁盘（2 条，不是 1 条）");
        t.check(reloaded.games().get(1).isHidden(), "★ 读回来仍是隐藏状态");

        GameLibrary hiddenCsv = library(game("GTA5", Platform.EPIC, 0.0, 8));
        hiddenCsv.find("GTA5", Platform.EPIC).orElseThrow().hide();
        Fixture hiddenCsvFix = fixture(hiddenCsv, "7\n2\n");
        hiddenCsvFix.run();
        try {
            String text = Files.readString(hiddenCsvFix.files.csvPath(), StandardCharsets.UTF_8);
            t.check(text.contains(",true"), "★ CSV 里 hidden 列是 true");
        } catch (IOException e) {
            t.check(false, "读 CSV 失败：" + e);
        }
    }

    private static void testSaveFailureKeepsRunning(TestRunner t) {
        t.section("[58] Menu：保存失败必须回菜单，不能退出");

        Path dir = tempDir();

        LibraryFiles broken = new LibraryFiles(
                dir.resolve("game_lib.csv"),
                dir.resolve("不存在的目录/game_lib.dat"),
                dir.resolve("game_lib_backup.dat"));

        GameLibrary lib = library(game("GTA5", Platform.EPIC, 0.0, 8));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Console console = new Console(
                new Scanner(new StringReader("7\n1\n7\n2\n")),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));

        new Menu(lib, reportFor(broken), broken, console).run();
        String out = buffer.toString(StandardCharsets.UTF_8);

        t.check(out.contains("【错误】"), "★ 打出了错误信息");
        t.check(out.contains("存档没有保存成功"), "★ 明确告知没保存成功");
        t.check(out.contains("正在写出 -> " + broken.csvPath()), "第二次保存真的写出了 CSV");
        t.check(out.contains("主菜单"), "★ 失败后回到了菜单，没有退出");
        t.check(out.contains("感谢使用"), "第二次选 CSV 成功后才退出");
        t.check(lib.size() == 1, "★ 库里的数据还在（没被清空）");
        t.check(Files.exists(broken.csvPath()), "第二次保存 CSV 成功");
    }

    private static void testBatchImport(TestRunner t) {
        t.section("[59] Menu：批量导入（§4.1）");

        Path dir = tempDir();
        Path incoming = dir.resolve("incoming.csv");
        writeCsv(incoming, List.of(
                game("Elden Ring", Platform.STEAM, 298.0, 10),
                game("GTA5", Platform.EPIC, 0.0, 8),
                game("Hades", Platform.STEAM, 70.0, 9)));

        // 库里已经有 Elden Ring -> 导入 3 条，1 条重复
        Fixture f = new Fixture(dir, library(game("Elden Ring", Platform.STEAM, 298.0, 10)),
                "1\n2\n" + incoming + "\n");
        f.run();

        String out = f.output();
        t.check(out.contains("从 CSV 文件批量导入"), "菜单里列出了批量导入选项");
        t.check(out.contains("导入完成"), "打印了导入回执");
        t.check(out.contains("新增 2 款"), "★ 新增 2 款 -> " + firstLineWith(out, "导入完成"));
        t.check(out.contains("跳过 1 款重复"), "★ 跳过 1 款重复");
        t.check(out.contains("Elden Ring | Steam"), "★ 列出了是哪一款重复");
        t.check(out.contains("当前库里共 3 款游戏"), "报告了导入后的总数");

        t.check(f.lib.size() == 3, "★ 库里最终 3 款");
        t.check(f.lib.find("GTA5", Platform.EPIC).isPresent(), "导入进来的能查到");
        t.check(f.lib.find("Hades", Platform.STEAM).isPresent(), "第二条也进来了");
        t.check(f.lib.find("Elden Ring", Platform.STEAM).orElseThrow()
                        .getPurchasePrice() == 298.0,
                "★ 重复那条没有覆盖库里原有的数据");
        t.check(out.contains("主菜单"), "导入后回到菜单");

        // 文件不存在：UncheckedIOException 被 handle 接住，程序不崩
        Fixture missing = new Fixture(tempDir(), new GameLibrary(),
                "1\n2\n" + dir.resolve("nope.csv") + "\n");
        missing.run();
        t.check(missing.output().contains("【错误】"), "★ 文件不存在时打错误信息");
        t.check(missing.output().contains("主菜单"), "★ 没崩，回到了菜单");

        // CSV 内容坏了：InvalidGameDataException 带行号
        Path broken = dir.resolve("broken.csv");
        writeRaw(broken, "title,platform\nElden Ring,Steam\n");
        Fixture badContent = new Fixture(tempDir(), new GameLibrary(), "1\n2\n" + broken + "\n");
        badContent.run();
        t.check(badContent.output().contains("【警告】"), "★ 格式不对时打警告");
        t.check(badContent.output().contains("第 1 行"), "★ 警告里带行号");

        // 路径里含 NUL -> InvalidPathException，被局部 catch 住
        Fixture badPath = new Fixture(tempDir(), new GameLibrary(), "1\n2\na\u0000b\n");
        badPath.run();
        t.check(badPath.output().contains("路径不合法") || badPath.output().contains("【错误】"),
                "★ 非法路径不崩");
        t.check(badPath.output().contains("主菜单"), "非法路径后回到菜单");

        // 空 CSV（只有表头）-> 新增 0，不报错
        Path headerOnly = dir.resolve("empty.csv");
        writeCsv(headerOnly, List.of());
        Fixture none = new Fixture(tempDir(), new GameLibrary(), "1\n2\n" + headerOnly + "\n");
        none.run();
        t.check(none.output().contains("新增 0 款"), "★ 空 CSV -> 新增 0 款");
        t.check(none.output().contains("跳过 0 款重复"), "没有重复");
    }

    private static void writeCsv(Path path, List<Game> games) {
        writeRaw(path, GameCsv.toCsvText(games));
    }

    private static void writeRaw(Path path, String text) {
        try {
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 取出输出里含某个关键词的第一行，断言失败时能看清实际内容。 */
    private static String firstLineWith(String output, String keyword) {
        for (String line : output.lines().toList()) {
            if (line.contains(keyword)) {
                return line.strip();
            }
        }
        return "（没找到含「" + keyword + "」的行）";
    }

    private static final class Fixture {

        private final LibraryFiles files;
        private final GameLibrary lib;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final Menu menu;

        Fixture(Path dir, GameLibrary lib, String script) {
            this.files = new LibraryFiles(
                    dir.resolve("game_lib.csv"),
                    dir.resolve("game_lib.dat"),
                    dir.resolve("game_lib_backup.dat"));
            this.lib = lib;
            Console console = new Console(
                    new Scanner(new StringReader(script)),
                    new PrintStream(buffer, true, StandardCharsets.UTF_8));
            this.menu = new Menu(lib, reportFor(files), files, console);
        }

        void run() {
            menu.run();
        }

        String output() {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private static Fixture fixture(GameLibrary lib, String script) {
        return new Fixture(tempDir(), lib, script);
    }

    private static LoadReport reportFor(LibraryFiles files) {
        try {
            return files.load();
        } catch (RuntimeException e) {
            return new LoadReport(LoadReport.Source.EMPTY, java.util.List.of(),
                    java.util.List.of("（测试夹具）"));
        }
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("glib-menu");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static GameLibrary library(Game... games) {
        GameLibrary lib = new GameLibrary();
        for (Game g : games) {
            lib.add(g);
        }
        return lib;
    }

    private static Game game(String title, Platform platform, double price, int rating) {
        return new Game(title, platform, "RPG", price,
                LocalDate.of(2020, 1, 1), rating, 25.0, "");
    }
}

package gamelib.ui;

import gamelib.io.LibraryFiles;
import gamelib.io.LoadReport;
import gamelib.model.Game;
import gamelib.model.InvalidGameDataException;
import gamelib.model.Platform;
import gamelib.store.GameLibrary;

import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Menu {

    private static final String MENU = """
            ===================================================
             主菜单
             1) 添加游戏
             2) 查看全部
             3) 搜索游戏
             4) 更新游戏
             5) 删除游戏
             6) 统计报表
             7) 保存并退出
            ---------------------------------------------------
             快速命令： add / search <关键词> / stats / exit
            ===================================================""";

    private static final double MAX_PRICE = 1_000_000;
    private static final double MAX_HOURS = 1_000_000;

    private static final int BAR_WIDTH = 12;

    private static final int LABEL_WIDTH = 16;

    private final GameLibrary lib;
    private final LoadReport report;
    private final LibraryFiles files;
    private final Console console;

    public Menu(GameLibrary lib, LoadReport report, LibraryFiles files, Console console) {
        this.lib = Objects.requireNonNull(lib, "lib 不能为 null");
        this.report = Objects.requireNonNull(report, "report 不能为 null");
        this.files = Objects.requireNonNull(files, "files 不能为 null");
        this.console = Objects.requireNonNull(console, "console 不能为 null");
    }

    public void run() {
        printStartup();

        boolean running = true;
        while (running) {
            console.println(MENU);
            if (!console.hasMore()) {
                break;
            }
            running = handle(console.readLine("> "));
        }
        console.println("感谢使用，再见 👋");
    }

    private void printStartup() {
        console.println("========== Personal Game Library Manager ==========");
        console.println("正在加载游戏库 ...");
        for (String warning : report.warnings()) {
            console.println("  " + warning);
        }
        console.println("===================================================");
    }

    private boolean handle(String line) {
        String[] parts = line.trim().split("\\s+", 2);
        String verb = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";

        try {
            switch (verb) {
                case "1", "add" -> doAdd();
                case "2", "list" -> doList();
                case "3", "search" -> doSearch(arg);
                case "4", "update" -> doUpdate();
                case "5", "delete" -> doDelete();
                case "6", "stats" -> doStats();
                case "7", "save", "exit", "quit" -> {
                    if (doSaveAndExit()) {
                        return false;
                    }
                }
                case "" -> {

                }
                default -> console.println("【警告】未知命令：" + verb + "（输入 7 退出）");
            }
        } catch (Console.CancelledException e) {
            console.println("已取消。");
        } catch (InvalidGameDataException e) {

            console.println("【警告】" + e.getMessage());
        } catch (UncheckedIOException e) {

            console.println("【错误】" + e.getMessage());
            console.println("  存档没有保存成功。修好问题后再选一次 7)。");
        }
        return true;
    }

    private void doAdd() {
        console.println("=> 添加游戏");

        Platform platform = Platform.fromString(
                console.readRequired("平台 (" + Platform.choices() + "): "));
        String title = console.readRequired("标题: ");
        String genre = console.readRequired("类型: ");
        double price = console.readDouble("购买价格 (¥): ", 0, MAX_PRICE);
        LocalDate purchaseDate = console.readDate("购买日期 (yyyy-MM-dd): ");
        boolean completed = console.readYesNo("是否已通关 (y/n): ");
        double hours = console.readDouble("游玩时长 (h): ", 0, MAX_HOURS);
        int rating = console.readInt(
                "个人评分 (" + Game.MIN_RATING + "-" + Game.MAX_RATING + "): ",
                Game.MIN_RATING, Game.MAX_RATING);
        String note = console.readLine("笔记 (可直接回车): ");

        Game game = new Game(title, platform, genre, price, purchaseDate, rating, hours, note);
        if (completed) {
            game.markCompleted();
        }

        lib.add(game);
        console.println("【提示】已成功入库：" + GameFormatter.brief(game));
    }

    private void doList() {
        console.println("=> 查看全部");

        List<Game> games = lib.all();
        if (games.isEmpty()) {
            if (lib.isEmpty()) {
                console.println("  库里还没有游戏。 ");
            } else {
                console.println("  没有可显示的游戏（" + lib.size() + " 款全部处于隐藏状态）。");
            }
            return;
        }
        console.print(GameFormatter.listing(games));
    }

    private void doSearch(String arg) {
        console.println("=> 搜索游戏");

        if (!arg.isBlank()) {
            showResults(lib.searchByTitle(arg), "标题含「" + arg + "」");
            return;
        }

        console.println(" 1) 按标题模糊搜索");
        console.println(" 2) 按平台筛选");
        console.println(" 3) 按类型筛选");
        console.println(" 4) 按通关状态筛选");
        console.println(" 5) 按价格区间筛选");
        int choice = console.readInt("> ", 1, 5);

        switch (choice) {
            case 1 -> showResults(lib.searchByTitle(console.readRequired("关键词: ")), "标题匹配");
            case 2 -> {
                Platform platform = Platform.fromString(
                        console.readRequired("平台 (" + Platform.choices() + "): "));
                showResults(lib.searchByPlatform(platform), "平台 = " + platform);
            }
            case 3 -> {
                Set<String> genres = lib.allGenres();
                console.println("  库里已有的类型："
                        + (genres.isEmpty() ? "（无）" : String.join("、", genres)));
                showResults(lib.searchByGenre(console.readRequired("类型关键词: ")), "类型匹配");
            }
            case 4 -> {
                boolean completed = console.readYesNo("只看已通关? (y/n): ");
                showResults(lib.searchByCompleted(completed), completed ? "已通关" : "未通关");
            }
            case 5 -> {
                double min = console.readDouble("最低价格 (¥): ", 0, MAX_PRICE);
                double max = console.readDouble("最高价格 (¥): ", min, MAX_PRICE);
                showResults(lib.searchByPriceRange(min, max), "价格 ¥" + min + " ~ ¥" + max);
            }
            default -> {

            }
        }
    }

    private void showResults(List<Game> games, String what) {
        if (games.isEmpty()) {
            console.println("  未找到匹配的游戏（" + what + "）。");
            return;
        }
        console.println("搜索结果（" + games.size() + " 条，" + what + "）：");
        for (String row : GameFormatter.lines(games)) {
            console.println("  - " + row);
        }
    }

    private void doUpdate() {
        console.println("=> 更新游戏");
        if (lib.isEmpty()) {
            console.println("  库里还没有游戏。");
            return;
        }

        Game game = promptForGame("输入要更新的「标题|平台」: ");

        while (true) {
            console.println("  当前：" + describe(game));
            console.println(" 1) 修改游玩时长    2) 标记为已通关");
            console.println(" 3) 更新个人评分    4) 记录笔记");
            console.println(" 0) 返回主菜单");

            int choice = console.readInt("> ", 0, 4);
            if (choice == 0) {
                return;
            }

            switch (choice) {
                case 1 -> {
                    double added = console.readDouble("输入新增小时: ", 0, MAX_HOURS);
                    game.addPlaytime(added);
                    console.println("【提示】更新后游玩时长: " + game.getPlaytimeHours() + " h");
                }
                case 2 -> {

                    LocalDate stamp = LocalDate.now();
                    LocalDate date = console.readDate(
                            "通关日期 (yyyy-MM-dd，回车 = " + stamp + "): ", stamp);
                    boolean wasCompleted = game.isCompleted();
                    try {
                        game.markCompleted(date);
                        console.println("【提示】" + (wasCompleted ? "通关日期已改为 " : "已标记通关，日期 ")
                                + game.getCompletedDate());
                    } catch (InvalidGameDataException e) {

                        console.println("【警告】" + e.getMessage());
                    }
                }
                case 3 -> {
                    game.updateRating(console.readInt(
                            "个人评分 (" + Game.MIN_RATING + "-" + Game.MAX_RATING + "): ",
                            Game.MIN_RATING, Game.MAX_RATING));
                    console.println("【提示】评分已更新：" + game.getRating());
                }
                case 4 -> {
                    console.println("  当前笔记："
                            + (game.getNotes().isEmpty() ? "（无）" : game.getNotes()));
                    String note = console.readLine("新笔记 (回车保持不变): ");
                    if (!note.isBlank()) {
                        game.setNotes(note);
                    }
                    console.println("【提示】笔记已更新。");
                }
                default -> {

                }
            }
        }
    }

    private void doDelete() {
        console.println("=> 删除游戏");
        if (lib.isEmpty()) {
            console.println("  库里还没有游戏。");
            return;
        }

        Game game = promptForGame("输入要操作的「标题|平台」: ");

        console.println(" 1) 永久删除（不可恢复）");
        console.println(" 2) 隐藏（可恢复，§4.4 扩展）");
        int choice = console.readInt("> ", 1, 2);

        if (choice == 2) {
            game.hide();
            console.println("【提示】已隐藏：" + GameFormatter.brief(game));
            return;
        }

        if (!console.confirm("确认永久删除 " + GameFormatter.brief(game) + " ?")) {
            console.println("已放弃删除。");
            return;
        }
        lib.remove(game.getTitle(), game.getPlatform());
        console.println("【提示】已删除：" + GameFormatter.brief(game));
    }

    private void doStats() {
        console.println("=> 统计报表");

        Stats s = Stats.of(lib);
        if (s.isEmpty()) {
            console.println("  库里还没有游戏，没有可统计的数据。");
            return;
        }

        console.println("-------------  个人游戏资产  -------------");
        console.println(GameFormatter.pad("总游戏数", LABEL_WIDTH) + ": " + s.totalGames());
        console.println(GameFormatter.pad("总花费", LABEL_WIDTH)
                + ": ¥" + String.format("%.2f", s.totalSpent()));
        console.println(GameFormatter.pad("平均评分", LABEL_WIDTH)
                + ": " + String.format("%.1f", s.averageRating()));

        console.println("各平台数量：");

        int max = s.countByPlatform().values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);
        s.countByPlatform().forEach((platform, count) -> {
            int bars = Math.max(1, (int) Math.round((double) BAR_WIDTH * count / max));
            console.println("  " + GameFormatter.pad(platform.toString(), 6)
                    + " " + "█".repeat(bars) + " " + count);
        });

        console.println(GameFormatter.pad("已通关", LABEL_WIDTH)
                + ": " + s.completedCount() + " 款");
        console.println(GameFormatter.pad("未通关", LABEL_WIDTH)
                + ": " + s.unfinishedCount() + " 款");

        if (!s.completedByYear().isEmpty()) {
            console.println("年度通关：");
            s.completedByYear().forEach((year, count) -> console.println(
                    "  " + GameFormatter.pad(year + " 年", 8) + ": " + count + " 款"));
        }

        s.mostExpensivePlatform().ifPresent(platform -> console.println(
                GameFormatter.pad("最烧钱的平台", LABEL_WIDTH) + ": " + platform
                        + "（¥" + String.format("%.2f", s.spendByPlatform().get(platform)) + "）"));

        console.println("-----------------------------------------");
    }

    private boolean doSaveAndExit() {
        console.println("=> 保存并退出");
        console.println("请选择保存格式：");
        console.println(" 1) 二进制（快速加载，私有格式）");
        console.println(" 2) CSV（可被 Excel 打开）");

        int choice = console.readInt("> ", 1, 2);

        List<Game> all = lib.allIncludingHidden();

        if (choice == 1) {
            files.saveBinary(all);
            console.println("正在写出 -> " + files.binaryPath() + " ...  完成！");
        } else {
            files.saveCsv(all);
            console.println("正在写出 -> " + files.csvPath() + " ...  完成！");
        }
        return true;
    }

    private Game promptForGame(String prompt) {
        while (true) {
            String raw = console.readRequired(prompt);

            String[] parts = raw.split("\\|", -1);
            if (parts.length != 2) {
                console.println("【警告】格式应为「标题|平台」，例如 GTA5|Epic");
                continue;
            }
            try {
                Platform platform = Platform.fromString(parts[1]);
                Optional<Game> found = lib.find(parts[0], platform);
                if (found.isEmpty()) {
                    console.println("【警告】没找到：" + parts[0] + " | " + platform);
                    continue;
                }
                return found.orElseThrow();
            } catch (InvalidGameDataException e) {
                console.println("【警告】" + e.getMessage());
            }
        }
    }

    private static String describe(Game game) {
        return GameFormatter.brief(game)
                + "（" + game.getPlaytimeHours() + "h"
                + "，评分 " + game.getRating()
                + (game.isCompleted() ? "，已通关" : "，未通关")
                + (game.isHidden() ? "，已隐藏" : "") + "）";
    }
}

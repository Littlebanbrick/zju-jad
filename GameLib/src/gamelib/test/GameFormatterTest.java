package gamelib.test;

import gamelib.model.Game;
import gamelib.model.Platform;
import gamelib.ui.GameFormatter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class GameFormatterTest {

    private GameFormatterTest() {
    }

    public static void run(TestRunner t) {
        testWidth(t);
        testPad(t);
        testLines(t);
        testChineseAlignment(t);
        testListing(t);
        testBriefAndNumbers(t);
    }

    private static void testWidth(TestRunner t) {
        t.section("[36] GameFormatter.width");

        t.check(GameFormatter.width("") == 0, "空串 -> 0");
        t.check(GameFormatter.width("abc") == 3, "纯 ASCII -> 字符数");
        t.check(GameFormatter.width("Elden Ring") == 10, "\"Elden Ring\" -> 10");
        t.check(GameFormatter.width("神作") == 4, "★ \"神作\" -> 4（不是 length() 的 2）");
        t.check(GameFormatter.width("巫师3") == 5, "★ \"巫师3\" -> 5（不是 3）");
        t.check(GameFormatter.width("。") == 2, "★ 中文句号 -> 2");
        t.check(GameFormatter.width("！") == 2, "★ 全角叹号 -> 2");
        t.check(GameFormatter.width("A，B") == 4, "★ 全角逗号 -> 2，所以 1+2+1=4");
        t.check(GameFormatter.width("¥") == 1, "★ 半角 ¥（U+00A5）算 1 列");
        t.check(GameFormatter.width("「」") == 4, "中文引号 -> 4");
    }

    private static void testPad(TestRunner t) {
        t.section("[37] GameFormatter.pad");

        t.check(GameFormatter.pad("abc", 5).equals("abc  "), "补到 5 -> 加 2 个空格");
        t.check(GameFormatter.pad("abc", 3).equals("abc"), "正好够 -> 原样");
        t.check(GameFormatter.pad("abcdef", 3).equals("abcdef"), "★ 超宽不截断");
        t.check(GameFormatter.pad("", 3).equals("   "), "空串 -> 3 个空格");
        t.check(GameFormatter.pad("神作", 6).equals("神作  "),
                "★ \"神作\" 已是 4 列 -> 只补 2 个空格（不是 4 个）");
        t.check(GameFormatter.pad("巫师3", 10).equals("巫师3     "),
                "★ \"巫师3\" 是 5 列 -> 补 5 个空格");

        t.check(GameFormatter.width(GameFormatter.pad("神作", 6)) == 6, "补齐后宽度 == 6");
        t.check(GameFormatter.width(GameFormatter.pad("abc", 8)) == 8, "补齐后宽度 == 8");
        t.check(GameFormatter.width(GameFormatter.pad("abcdef", 3)) == 6, "超宽时宽度不变");
    }

    private static void testLines(TestRunner t) {
        t.section("[38] GameFormatter.lines");

        t.check(GameFormatter.lines(List.of()).isEmpty(), "空列表 -> 空 List（不是 null）");

        List<Game> games = List.of(
                game("Elden Ring", Platform.STEAM, 298.0, 120.0, 10, true, "神作，DLC快出！"),
                game("GTA5", Platform.EPIC, 0.0, 30.0, 8, true, "Epic白嫖"));

        List<String> lines = GameFormatter.lines(games);
        t.check(lines.size() == 2, "两条");

        t.check(lines.get(0).equals(
                        "Elden Ring | Steam | ¥298 | 120h | 评分 10 | 已通关 | 神作，DLC快出！"),
                "第 1 行与 §2 示例逐字符一致 -> " + lines.get(0));
        t.check(lines.get(1).equals(
                        "GTA5       | Epic  | ¥0   | 30h  | 评分 8  | 已通关 | Epic白嫖"),
                "第 2 行与 §2 示例逐字符一致 -> " + lines.get(1));

        List<String> one = GameFormatter.lines(List.of(
                game("Hades", Platform.STEAM, 70.0, 40.0, 9, false, "")));
        t.check(one.get(0).equals("Hades | Steam | ¥70 | 40h | 评分 9  | 未通关"),
                "★ 笔记为空时不留悬空的 \" | \" -> " + one.get(0));

        List<String> platform = GameFormatter.lines(List.of(
                game("A", Platform.XBOX, 1.0, 1.0, 5, false, "")));
        t.check(platform.get(0).contains("Xbox"), "平台用 label（Xbox）而不是 XBOX");
        t.check(!platform.get(0).contains("XBOX"), "不出现常量名");
    }

    private static void testChineseAlignment(TestRunner t) {
        t.section("[39] 中文标题对齐（用显示宽度量，不靠肉眼）");

        List<Game> games = List.of(
                game("巫师3", Platform.GOG, 99.0, 60.0, 9, false, ""),
                game("Elden Ring", Platform.STEAM, 298.0, 120.0, 10, true, ""),
                game("神作", Platform.EPIC, 0.0, 5.0, 8, false, ""));

        List<String> lines = GameFormatter.lines(games);
        t.check(lines.size() == 3, "三条");

        int first = alignedPrefixWidth(lines.get(0));
        t.check(first > 0, "能定位到前 4 个分隔符");
        t.check(alignedPrefixWidth(lines.get(1)) == first, "★ ASCII 行的列边界一致");
        t.check(alignedPrefixWidth(lines.get(2)) == first, "★ 中文行的列边界也一致");

        t.check(lines.get(0).startsWith("巫师3 "), "「巫师3」后面开始补空格");

        int pipeCol0 = GameFormatter.width(lines.get(0).substring(0, lines.get(0).indexOf('|')));
        int pipeCol1 = GameFormatter.width(lines.get(1).substring(0, lines.get(1).indexOf('|')));
        int pipeCol2 = GameFormatter.width(lines.get(2).substring(0, lines.get(2).indexOf('|')));
        t.check(pipeCol0 == pipeCol1 && pipeCol1 == pipeCol2,
                "★ 三条行的第一个 '|' 显示列位置相同（" + pipeCol0 + "/" + pipeCol1
                        + "/" + pipeCol2 + "）");
        t.check(lines.get(0).indexOf('|') != lines.get(1).indexOf('|'),
                "（反面对照：中文行的字符下标本来就比 ASCII 行小，这正说明不能用下标比）");

        t.check(GameFormatter.width("评分") == 4, "「评分」是 4 列");
    }

    private static void testListing(TestRunner t) {
        t.section("[40] GameFormatter.listing");

        String empty = GameFormatter.listing(List.of());
        t.check(empty.contains("共 0 款游戏"), "空列表 -> 「共 0 款游戏」");
        t.check(empty.lines().count() == 1, "空列表只有那一行");

        List<Game> two = List.of(
                game("Elden Ring", Platform.STEAM, 298.0, 120.0, 10, true, ""),
                game("GTA5", Platform.EPIC, 0.0, 30.0, 8, false, ""));
        String listing = GameFormatter.listing(two);
        t.check(listing.contains("共 2 款游戏"), "含「共 2 款游戏」");
        t.check(listing.contains("1. Elden Ring | Steam | ¥298 | 120h | 评分 10 | 已通关"),
                "第 1 行带编号 -> " + listing.lines().toList().get(1));
        t.check(listing.contains("2. GTA5       | Epic  | ¥0   | 30h  | 评分 8  | 未通关"),
                "第 2 行带编号");

        List<Game> many = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            many.add(game("Game" + i, Platform.STEAM, 1.0, 1.0, 5, false, ""));
        }
        String big = GameFormatter.listing(many);
        List<String> rows = big.lines()
                .filter(l -> l.contains(". "))
                .toList();
        t.check(rows.size() == 12, "12 行");
        int pos1 = rows.get(0).indexOf("Game1");
        int pos12 = rows.get(11).indexOf("Game12");
        t.check(pos1 == pos12, "★ 编号 1 和 12 之后标题起始位置相同（1 -> " + pos1
                + "，12 -> " + pos12 + "）");
    }

    private static void testBriefAndNumbers(TestRunner t) {
        t.section("[41] GameFormatter.brief 与数值格式");

        Game g = game("Elden Ring", Platform.STEAM, 298.0, 120.0, 10, true, "");
        t.check(GameFormatter.brief(g).equals("Elden Ring | Steam"),
                "brief 是「标题 | 平台」且不带两侧空格 -> [" + GameFormatter.brief(g) + "]");

        List<String> whole = GameFormatter.lines(List.of(
                game("A", Platform.STEAM, 298.0, 120.0, 5, false, "")));
        t.check(whole.get(0).contains("¥298"), "★ 整数价格 298.0 -> \"¥298\"（不带小数）");
        t.check(whole.get(0).contains(" 120h "), "★ 整数时长 120.0 -> \"120h\"");

        List<String> fraction = GameFormatter.lines(List.of(
                game("B", Platform.STEAM, 15.5, 15.5, 5, false, "")));
        t.check(fraction.get(0).contains("¥15.50"),
                "★ 15.5 -> \"¥15.50\" 而不是 \"¥16\"（不能悄悄改数据）-> " + fraction.get(0));
        t.check(fraction.get(0).contains("15.5h"), "15.5 -> \"15.5h\"");

        List<String> zero = GameFormatter.lines(List.of(
                game("C", Platform.EPIC, 0.0, 0.0, 0, false, "")));
        t.check(zero.get(0).contains("¥0"), "0.0 -> \"¥0\"");
        t.check(zero.get(0).contains("0h"), "0.0 -> \"0h\"");
        t.check(zero.get(0).contains("评分 0 "), "评分 0 补到两位");

        t.check(whole.get(0).equals("A | Steam | ¥298 | 120h | 评分 5  | 未通关"),
                "单条无多余空格 -> " + whole.get(0));
    }

    private static int alignedPrefixWidth(String line) {
        int seen = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.startsWith(" | ", i)) {
                seen++;
                if (seen == 4) {
                    return GameFormatter.width(line.substring(0, i + 3));
                }
            }
        }
        return -1;
    }

    private static Game game(String title, Platform platform, double price, double hours,
                             int rating, boolean completed, String note) {
        Game g = new Game(title, platform, "RPG", price,
                LocalDate.of(2020, 1, 1), rating, hours, note);
        if (completed) {
            g.markCompleted(LocalDate.of(2024, 1, 1));
        }
        return g;
    }
}

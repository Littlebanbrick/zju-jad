package gamelib.ui;

import gamelib.model.Game;

import java.util.List;
import java.util.ArrayList;

// 返回字符串，交给Menu打印
// 我们只考虑中英文字符和ascii符号，其他字符不保证对齐
public class GameFormatter {
    private GameFormatter() {}

    /** 终端显示宽度：汉字和全角符号算 2 列，其余算 1 列。 */
    public static int width(String s) {
        int width = 0;
        for (char c: s.toCharArray()) {
            width += isWide(c) ? 2 : 1;
        }
        return width;
    }

    /** 右补空格。已经够宽就原样返回，不截断。 */
    public static String pad(String s, int targetWidth) {
        int currentWidth = width(s);
        if (currentWidth >= targetWidth) {
            return s;
        }

        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < targetWidth - currentWidth; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** 对齐后的行，不含编号。搜索结果用它。 */
    public static List<String> lines(List<Game> games) {
        int[] widths = columnWidths(games);
        int titleWidth = widths[0];
        int platformWidth = widths[1];
        int priceWidth = widths[2];
        int hoursWidth = widths[3];

        List<String> lines = new ArrayList<>();

        for(Game g: games) {
            String title = pad(g.getTitle(), titleWidth);
            String platform = pad(g.getPlatform().toString(), platformWidth);
            String price = pad(price(g), priceWidth);
            String hours = pad(hours(g), hoursWidth);

            String line = title + " | " + platform + " | " + price + " | " + hours
                        + " | 评分 " + pad(String.valueOf(g.getRating()), 2)
                        + " | " + (g.isCompleted() ? "已通关" : "未通关");

            // 空笔记不留悬空的 " | "
            String note = g.getNotes();
            if (!note.isEmpty()) {
                line += " | " + note;
            }

            lines.add(line);
        }
        return lines;
    }

    /** 「共 N 款游戏」+ 带编号的行。查看全部用它。 */
    public static String listing(List<Game> games) {
        List<String> lines = lines(games);
        // 编号宽度按总数算：否则 10 款以上时「1. 」和「10. 」会把标题列推歪
        int numWidth = String.valueOf(games.size()).length();

        StringBuilder sb = new StringBuilder();
        sb.append("共 ").append(games.size()).append(" 款游戏\n");
        for (int i = 0; i < lines.size(); i++) {
            sb.append(String.format("%" + numWidth + "d. %s\n", i + 1, lines.get(i)));
        }
        return sb.toString();
    }

    /** 单条紧凑格式，不补齐。用于「已成功入库：Elden Ring | Steam」。 */
    public static String brief(Game g) {
        return g.getTitle() + " | " + g.getPlatform();
    }

    private static boolean isWide(char c) {
        return (c >= 0x3000 && c <= 0x303F)     // CJK 标点：。、〈〉「」
            || (c >= 0x4E00 && c <= 0x9FFF)     // 汉字
            || (c >= 0xFF00 && c <= 0xFF60)     // 全角 ASCII：，！？：；（）ＡＢ
            || (c >= 0xFFE0 && c <= 0xFFE6);    // 全角符号：￠￡￥￦
    }

    private static String price(Game g) {
        double price = g.getPurchasePrice();
        if (price == (int) price) {
            return String.format("¥%d", (int) price);
        } else {
            return String.format("¥%.2f", price);
        }
    }

    private static String hours(Game g) {
        double hours = g.getPlaytimeHours();
        if (hours == (int) hours) {
            return String.format("%dh", (int) hours);
        } else {
            return String.format("%.1fh", hours);
        }
    }

    private static int[] columnWidths(List<Game> games) { // 4 列的宽度：title/platform/price/hours
        int[] widths = new int[4];
        for(Game g: games) {
            widths[0] = Math.max(widths[0], width(g.getTitle()));
            widths[1] = Math.max(widths[1], width(g.getPlatform().toString()));
            widths[2] = Math.max(widths[2], width(price(g)));
            widths[3] = Math.max(widths[3], width(hours(g)));
        }
        return widths;
    }
}

package gamelib.ui;

import gamelib.model.Game;

import java.util.List;
import java.util.ArrayList;

public class GameFormatter {
    // 工具类，不用实例化
    private GameFormatter() {}

    public static int width(String s) {
        int width = 0;
        for (char c: s.toCharArray()) {
            width += isWide(c) ? 2 : 1;
        }
        return width;
    }

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

            String note = g.getNotes();
            if (!note.isEmpty()) {
                line += " | " + note;
            }

            lines.add(line);
        }
        return lines;
    }

    public static String listing(List<Game> games) {
        List<String> lines = lines(games);

        int numWidth = String.valueOf(games.size()).length();

        StringBuilder sb = new StringBuilder();
        sb.append("共 ").append(games.size()).append(" 款游戏\n");
        for (int i = 0; i < lines.size(); i++) {
            sb.append(String.format("%" + numWidth + "d. %s\n", i + 1, lines.get(i)));
        }
        return sb.toString();
    }

    public static String brief(Game g) {
        return g.getTitle() + " | " + g.getPlatform();
    }

    // 用于判断一个字符在等宽终端里占 1 列还是 2 列
    private static boolean isWide(char c) {
        return (c >= 0x3000 && c <= 0x303F)     // CJK标点
            || (c >= 0x4E00 && c <= 0x9FFF)     // 汉字
            || (c >= 0xFF00 && c <= 0xFF60)     // 全角 ascii
            || (c >= 0xFFE0 && c <= 0xFFE6);    // 全角符号
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

    private static int[] columnWidths(List<Game> games) {
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

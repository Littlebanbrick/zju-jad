package gamelib.io;

import gamelib.model.Game;
import gamelib.model.Platform;
import gamelib.model.InvalidGameDataException;

import java.util.List;
import java.time.LocalDate;

public final class GameCsv {
    // 同样的，GameCsv 也是一个“工具类”，用于处理 Game 对象和 CSV 之间的转换
    // 因此构造器private
    private GameCsv() {}

    // csv文件的表头字段顺序和名称是固定的，定义为常量
    public static final List<String> HEADER = List.of(
        "title",
        "platform",
        "genre",
        "purchase_price",
        "purchase_date",
        "completed",
        "completed_date",
        "playtime_hours",
        "rating",
        "note",
        "hidden"
    );

    public static List<String> toFields(Game g) {
        List<String> fields = List.of(
            g.getTitle(),
            g.getPlatform().name(),
            g.getGenre(),
            String.valueOf(g.getPurchasePrice()),
            g.getPurchaseDate().toString(),
            g.isCompleted() ? "y" : "n",
            g.getCompletedDate() == null ? "" : String.valueOf(g.getCompletedDate()),
            String.valueOf(g.getPlaytimeHours()),
            String.valueOf(g.getRating()),
            g.getNotes(),
            (g.isHidden() ? "true" : "false")
        );
        return fields;
    }

    public static Game fromFields(List<String> fields, int lineNumber) {

        if (fields.size() != HEADER.size()) {
            throw new InvalidGameDataException("CSV 第 " + lineNumber + " 行字段数不对：期望 "
                    + HEADER.size() + "，实际 " + fields.size());
        }

        String title;
        try {
            title = fields.get(0);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        Platform platform;
        String platformStr;
        try {
            platformStr = fields.get(1);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        try {
            platform = Platform.fromString(platformStr);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("第 " + lineNumber + " 行 platform 字段不合法: " + platformStr);
        }

        String genre;
        try {
            genre = fields.get(2);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        String purchasePriceStr;
        try {
            purchasePriceStr = fields.get(3);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        // 这里使用正则表达式检查 purchasePriceStr 是否是一个数字（整数或小数）
        // 简单的正则语法：
        // \d  匹配一个数字（0-9）
        // \d+ 匹配一个或多个数字
        // ()? 匹配括号内的内容 0 次或 1 次
        // \.  匹配小数点（需要转义，因为 . 在正则中有特殊含义）
        // 
        // matches() 方法用于检查字符串是否匹配给定的正则表达式
        if (!purchasePriceStr.matches("\\d+(\\.\\d+)?")) {
            throw new InvalidGameDataException("第 " + lineNumber + " 行 purchase_price 不是数字: " + purchasePriceStr);
        }

        double purchasePrice;
        try {
            // parseDouble() 是 Double 类的静态方法，用于将字符串解析为 double 类型的数值
            purchasePrice = Double.parseDouble(purchasePriceStr);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("第 " + lineNumber + " 行 purchase_price 不是数字: " + purchasePriceStr);
        }

        String purchaseDateStr;
        try {
            purchaseDateStr = fields.get(4);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        LocalDate purchaseDate;
        try {
            purchaseDate = LocalDate.parse(purchaseDateStr);
        } catch (Exception e) {
            throw new InvalidGameDataException("第 " + lineNumber + " 行 purchase_date 格式不合法: " + purchaseDateStr);
        }

        String playtimeHoursStr;
        try {
            playtimeHoursStr = fields.get(7);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        if (!playtimeHoursStr.matches("\\d+(\\.\\d+)?")) {
            throw new InvalidGameDataException("第 " + lineNumber + " 行 playtime_hours 不是数字: " + playtimeHoursStr);
        }
        double playtimeHours;
        try {
            // parseDouble() 是 Double 类的静态方法，用于将字符串解析为 double 类型的数值
            playtimeHours = Double.parseDouble(playtimeHoursStr);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("第 " + lineNumber + " 行 playtime_hours 不是数字: " + playtimeHoursStr);
        }

        String ratingStr;
        try {
            ratingStr = fields.get(8);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }
        int rating;
        try {
            // parseInt() 方法用于将字符串解析为 int 类型的数值
            rating = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            throw new InvalidGameDataException("第 " + lineNumber + " 行 rating 不是整数: " + ratingStr);
        }

        String note;
        try {
            note = fields.get(9);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        Game game;
        try {
            game = new Game(
                title,
                platform,
                genre,
                purchasePrice,
                purchaseDate,
                rating,
                playtimeHours,
                note
            );
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("CSV 第 " + lineNumber + " 行数据不合法: " + e.getMessage());
        }

        String completedDateStr;
        try {
            completedDateStr = fields.get(6);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        if (!completedDateStr.isBlank()) {
            LocalDate completedDate = LocalDate.parse(completedDateStr);
            try {
                game.markCompleted(completedDate);
            } catch (InvalidGameDataException e) {
                throw new InvalidGameDataException("CSV 第 " + lineNumber
                        + " 行「completed_date」列：" + e.getMessage(), e);
            }
        }

        String hiddenStr;
        try {
            hiddenStr = fields.get(10);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        if(hiddenStr.equalsIgnoreCase("true")) {
            game.hide();
        } else if(hiddenStr.equalsIgnoreCase("false")) {
            game.unhide();
        } else {
            throw new InvalidGameDataException("CSV 第 " + lineNumber + " 行 hidden 字段不合法: " + hiddenStr);
        }

        if (fields.size() > 11) {
            throw new InvalidGameDataException("CSV 第 " + lineNumber + " 行字段数过多，期望 11 个，实际 " + fields.size());
        }

        return game;
    }

    // 把一堆 Game 对象转换为 CSV 文本
    public static String toCsvText(List<Game> games) {
        StringBuilder sb = new StringBuilder();

        sb.append(Csv.BOM);

        // 添加表头行
        sb.append(Csv.formatRecord(HEADER)).append('\n');
        for (Game g: games) {
            sb.append(Csv.formatRecord(toFields(g))).append('\n');
        }
        return sb.toString();
    }

    // toCsvText() 的逆操作，把 CSV 文本解析为 Game 对象列表
    public static List<Game> parseCsvText(String text) {

        String normalized = Csv.stripBom(text);
        if (normalized.isBlank()) {
            return new java.util.ArrayList<>();
        }

        List<String> lines = List.of(normalized.split("\n"));
        List<String> headerFields = Csv.parseLine(lines.get(0));
        if (!headerFields.equals(HEADER)) {

            if (headerFields.size() == HEADER.size() && headerFields.containsAll(HEADER)) {
                throw new InvalidGameDataException("CSV 第 1 行列字段顺序错误：期望 " + HEADER
                        + "，实际 " + headerFields);
            }
            throw new InvalidGameDataException("CSV 第 1 行表头不对：期望 " + HEADER
                    + "，实际 " + headerFields);
        }

        List<Game> games = new java.util.ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            List<String> fields = Csv.parseLine(line);
            Game g = fromFields(fields, i + 1);
            games.add(g);
        }
        return games;
    }
}

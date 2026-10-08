package gamelib.io;

import gamelib.model.Game;
import gamelib.model.Platform;
import gamelib.model.InvalidGameDataException;

import java.util.List;
import java.time.LocalDate;

public final class GameCsv {
    private GameCsv() {}

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

        if (!purchasePriceStr.matches("\\d+(\\.\\d+)?")) {
            throw new InvalidGameDataException("第 " + lineNumber + " 行 purchase_price 不是数字: " + purchasePriceStr);
        }

        String purchaseDateStr;
        try {
            purchaseDateStr = fields.get(4);
        } catch (InvalidGameDataException e) {
            throw new InvalidGameDataException("字段数可能不对，第 " + lineNumber + " 行: " + e.getMessage());
        }

        try {
            LocalDate.parse(purchaseDateStr);
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
                Double.parseDouble(purchasePriceStr),
                java.time.LocalDate.parse(purchaseDateStr),
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

    public static String toCsvText(List<Game> games) {
        StringBuilder sb = new StringBuilder();

        sb.append(Csv.BOM);

        sb.append(Csv.formatRecord(HEADER)).append('\n');
        for (Game g: games) {
            sb.append(Csv.formatRecord(toFields(g))).append('\n');
        }
        return sb.toString();
    }

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

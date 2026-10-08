package gamelib.io;

import gamelib.model.InvalidGameDataException;

import java.util.ArrayList;
import java.util.List;

public final class Csv {

    public static final String BOM = "\uFEFF";

    private Csv() {
    }

    public static String escape(String field) {
        if (field.indexOf(',') < 0
                && field.indexOf('"') < 0) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }

    public static String formatRecord(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields.get(i)));
        }
        return sb.toString();
    }

    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();

        boolean inQuotes = false;
        boolean atFieldStart = true;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }

            if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                atFieldStart = true;
                continue;
            }

            if (c == '"' && atFieldStart) {
                inQuotes = true;
                atFieldStart = false;
                continue;
            }

            field.append(c);
            atFieldStart = false;
        }

        if (inQuotes) {
            throw new InvalidGameDataException(
                    "CSV 引号未闭合（字段里可能混进了换行）：" + preview(line));
        }

        fields.add(field.toString());
        return fields;
    }

    public static String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    private static String preview(String line) {
        return line.length() <= 60 ? line : line.substring(0, 60) + "...";
    }
}

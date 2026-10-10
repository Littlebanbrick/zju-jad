package gamelib.io;

import gamelib.model.InvalidGameDataException;

import java.util.ArrayList;
import java.util.List;

public final class Csv {

    // BOM: Byte Order Mark, 用于标识 UTF-8 编码的文件开头（写在文件最前面）
    // 参考：https://en.wikipedia.org/wiki/Byte_order_mark
    // 有 BOM 才会认 UTF-8，否则默认本地编码，可能导致乱码
    public static final String BOM = "\uFEFF";

    // 我们发现 Csv 只是一个“工具类”：里面全是静态方法，没有状态，也不需要实例化对象
    // 所以我们把构造函数设为 private，防止外部实例化
    private Csv() {}

    // escape v.转义
    public static String escape(String field) {
        // indexOf() 方法返回指定字符在字符串中第一次出现的索引，如果没有找到则返回 -1
        if (field.indexOf(',') < 0 && field.indexOf('"') < 0) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }

    // 这个方法把一个字段列表格式化为 CSV 记录（即一行文本），并返回该行文本
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

    // 这个方法解析 CSV 行，返回字段列表
    // 它是 formatRecord() 的逆操作
    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();

        boolean inQuotes = false;
        boolean atFieldStart = true;

        for (int i = 0; i < line.length(); i++) {
            // chatAt() 方法返回指定索引处的字符
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
                // setLength(0) 方法将 StringBuilder 的长度设置为指定值（截断），
                // 短了补 \0，长了截断，长度为 0 就是清空，负数抛异常
                // 这里设置为 0，相当于清空了 StringBuilder
                // 不每次新建 SB 可以复用缓冲区，快
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

    // java中，一个char是两个bytes，因此 \uFEFF 是一个char
    public static String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            // subString() 方法返回一个新的字符串，它是此字符串的一个子字符串
            // 调用示例：
            // String str = "Hello, World!";
            // String substr = str.substring(7); // substr = "World!"，从索引7开始截取到字符串末尾
            return line.substring(1);
        }
        return line;
    }

    private static String preview(String line) {
        return line.length() <= 60 ? line : line.substring(0, 60) + "...";
    }
}

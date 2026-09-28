package gamelib.io;

import gamelib.model.InvalidGameDataException;

import java.util.ArrayList;
import java.util.List;

/**
 * RFC 4180 的最小子集：单行 CSV 的转义与解析。
 *
 * <p>这一层完全不认识 {@code Game}，只处理「字符串 &lt;-&gt; 字段列表」，
 * 所以转义规则可以用纯字符串测试打穿，不需要构造任何 Game 对象。
 *
 * <p><b>简化声明</b>：字段内的换行不受支持。因为录入路径只有
 * {@code Scanner.nextLine()}，且 {@link gamelib.model.Game} 会把换行归一化成空格，
 * 所以本项目的字段永远是单行。代价是含换行的引号字段（外部 CSV）
 * 解析不了 —— 这时 {@link #parseLine} 抛异常，而不是静默产出脏数据。
 *
 * <p><b>规则</b>：
 * <ul>
 *   <li>字段含 {@code ,} 或 {@code "} 时，整个字段用 {@code "} 包起来</li>
 *   <li>字段内的 {@code "} 写成 {@code ""}（对翻倍 —— CSV 没有反斜杠转义）</li>
 *   <li>引号内遇到 {@code "} 时看下一个字符：还是 {@code "} 就是字面引号，
 *       否则就是字段收尾（这一条是整个格式的关键，见 {@link #parseLine}）</li>
 * </ul>
 */
public final class Csv {

    /** UTF-8 BOM。写给 Excel 看时必须加，读回来时必须剥掉。 */
    public static final String BOM = "\uFEFF";

    private Csv() {
    }

    /**
     * 转义单个字段，必要时加引号，内部的 {@code "} 对翻倍。
     *
     * <p>调用方必须传 {@code ""} 而不是 {@code null}（{@code cover_image_path}
     * 可能为空，在 {@code GameCsv} 里负责转换）。
     *
     * <p>字段含换行时不会加引号 —— 见类注释的简化声明。
     */
    public static String escape(String field) {
        if (field.indexOf(',') < 0
                && field.indexOf('"') < 0) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }

    /** 把一个记录拼成一行。字段顺序由调用方决定。 */
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

    /**
     * 解析一行，返回它的字段列表。
     *
     * <p>状态机逐字符扫描，有三个状态：字段起始 / 字段中间 / 引号内。
     * <ul>
     *   <li>不在引号内遇到 {@code ,} → 结束当前字段</li>
     *   <li>字段起始处遇到 {@code "} → 进入引号模式</li>
     *   <li>引号内遇到 {@code ""} → 追加一个字面 {@code "}；单独的 {@code "} → 退出引号模式</li>
     *   <li>引号内的 {@code ,} 原样追加（这就是必须用状态机而不能 {@code split(",")} 的原因）</li>
     * </ul>
     *
     * <p>{@code ""}（空行）返回 {@code [""]} —— 一个空字段。调用方应自行跳过空行。
     *
     * <p>传入的行不应带行结束符；{@code Files.readAllLines} 已经处理掉了。
     *
     * @throws InvalidGameDataException 引号未闭合（这一行多半来自一个含换行的字段，
     *         也就是被外部 CSV 破坏了）
     */
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
                        i++;            // 吃掉第二个引号
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);    // 引号内的逗号在这里被原样保留
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

    /**
     * 剥掉行首的 BOM。
     *
     * <p>必须手动剥：{@code \uFEFF} 不算空白字符，{@code String.strip()} 去不掉它。
     * 不剥的后果是第一个表头字段变成 {@code "\uFEFFtitle"}，按列名找 {@code title} 会失败。
     * 只有文件首行会带 BOM，调用方对第一行调一次即可。
     */
    public static String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    /** 报错信息里只放一小段，免得一整行垃圾刷屏。 */
    private static String preview(String line) {
        return line.length() <= 60 ? line : line.substring(0, 60) + "...";
    }
}

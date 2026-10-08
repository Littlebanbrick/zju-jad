package gamelib.test;

import gamelib.io.Csv;

import java.util.ArrayList;
import java.util.List;

public final class CsvTest {

    private CsvTest() {
    }

    public static void run(TestRunner t) {
        testEscape(t);
        testParseLine(t);
        testBom(t);
        testRoundTrip(t);
    }

    private static void testEscape(TestRunner t) {
        t.section("[19] Csv.escape / formatRecord");

        t.check(Csv.escape("普通文本").equals("普通文本"), "不含特殊字符 -> 不加引号");
        t.check(Csv.escape("").equals(""), "空串 -> 空串");
        t.check(Csv.escape("含,逗号").equals("\"含,逗号\""), "含逗号 -> 加引号");
        t.check(Csv.escape("含\"引号").equals("\"含\"\"引号\""), "含引号 -> 加引号且翻倍");
        t.check(Csv.escape("\"").equals("\"\"\"\""), "单个引号 -> 四个引号");
        t.check(Csv.escape("神作\", DLC别买").equals("\"神作\"\", DLC别买\""),
                "同时含引号和逗号");

        t.check(Csv.formatRecord(List.of()).equals(""), "空记录 -> 空行");
        t.check(Csv.formatRecord(List.of("a")).equals("a"), "单字段");
        t.check(Csv.formatRecord(List.of("a", "b", "c")).equals("a,b,c"), "多字段用逗号连接");
        t.check(Csv.formatRecord(List.of("a", "", "c")).equals("a,,c"), "中间空字段");
        t.check(Csv.formatRecord(List.of("", "")).equals(","), "两个空字段");
        t.check(Csv.formatRecord(List.of("a", "b,c")).equals("a,\"b,c\""),
                "只有需要的那一个字段加引号");
    }

    private static void testParseLine(TestRunner t) {
        t.section("[20] Csv.parseLine");

        t.check(Csv.parseLine("a,b,c").equals(List.of("a", "b", "c")), "普通三字段");
        t.check(Csv.parseLine("a").equals(List.of("a")), "单字段");
        t.check(Csv.parseLine("").equals(List.of("")), "空行 -> 一个空字段（调用方跳过空行）");
        t.check(Csv.parseLine("a,b,,d").equals(List.of("a", "b", "", "d")),
                "★ 中间和末尾的空字段都不能丢（split(\",\") 会丢）");
        t.check(Csv.parseLine("a,").equals(List.of("a", "")), "★ 末尾空字段保留");
        t.check(Csv.parseLine(",").equals(List.of("", "")), "两个空字段");

        t.check(Csv.parseLine("\"a,b\",c").equals(List.of("a,b", "c")),
                "★ 引号内的逗号不是分隔符");
        t.check(Csv.parseLine("a,\"b,c\"").equals(List.of("a", "b,c")), "第二个字段带引号");
        t.check(Csv.parseLine("\"\"\"\"").equals(List.of("\"")), "★ 四个引号 -> 一个字面引号");
        t.check(Csv.parseLine("\"say \"\"hi\"\"\"").equals(List.of("say \"hi\"")),
                "★ 翻倍引号还原成一个");
        t.check(Csv.parseLine("\"\"").equals(List.of("")), "空引号 -> 空字段");
        t.check(Csv.parseLine("\"a\"\"b\",c").equals(List.of("a\"b", "c")),
                "引号内的翻倍 + 引号后的逗号");

        t.check(Csv.parseLine("  a  ").equals(List.of("  a  ")),
                "不 trim：前后空格是数据的一部分");
        t.check(Csv.parseLine("\"a,b\",\"c,d\"").equals(List.of("a,b", "c,d")),
                "相邻两个带引号字段");

        t.expectInvalid("★ 引号未闭合 -> 抛异常（外部 CSV 含换行的信号）",
                () -> Csv.parseLine("\"续行内容没有收尾引号"));
        t.expectInvalid("引号未闭合（带逗号）",
                () -> Csv.parseLine("a,\"b,c"));
    }

    private static void testBom(TestRunner t) {
        t.section("[21] BOM 处理");

        t.check(Csv.BOM.equals("\uFEFF"), "BOM 常量就是 U+FEFF");
        t.check(Csv.stripBom(Csv.BOM + "title,note").equals("title,note"), "剥掉行首 BOM");
        t.check(Csv.stripBom("title").equals("title"), "没有 BOM 时原样返回");
        t.check(Csv.stripBom("").equals(""), "空串安全");
        t.check(Csv.stripBom("a" + Csv.BOM + "b").equals("a" + Csv.BOM + "b"),
                "只剥行首，中间的不动");

        t.check((Csv.BOM + "title").strip().startsWith(Csv.BOM),
                "★ String.strip() 去不掉 BOM（所以必须手动剥）");

        List<String> withoutStrip = Csv.parseLine(Csv.BOM + "title,note");
        t.check(!withoutStrip.get(0).equals("title"), "★ 不剥 BOM：列名变成 \\uFEFFtitle");
        List<String> withStrip = Csv.parseLine(Csv.stripBom(Csv.BOM + "title,note"));
        t.check(withStrip.get(0).equals("title"), "剥了 BOM 之后列名才是 title");
    }

    private static void testRoundTrip(TestRunner t) {
        t.section("[22] round-trip：写出去的必须能原样读回来");

        String[] nasty = {
            "",
            "普通文本",
            "含,半角逗号",
            "含，全角逗号",
            "含\"双引号",
            "神作\", DLC别买",
            "\"\"",
            "\"",
            ",",
            ",,",
            "a,b,c",
            "  前后有空格  ",
            "Epic白嫖",
            "C:\\games\\save",
        };

        for (String s : nasty) {
            List<String> original = List.of(s);
            String line = Csv.formatRecord(original);
            t.check(Csv.parseLine(line).equals(original),
                    "round-trip [" + s + "]  ->  " + line);
        }

        List<String> record = List.of(
                "Elden Ring", "Steam", "ARPG", "298.0", "2022-03-01",
                "y", "2024-03-15", "120.0", "10", "神作\", DLC别买", "", "false");
        String line = Csv.formatRecord(record);
        List<String> back = Csv.parseLine(line);
        t.check(back.equals(record), "整条 12 列记录的 round-trip");
        t.check(back.size() == 12, "读回来仍是 12 列");
        t.check(back.get(9).equals("神作\", DLC别买"), "第 10 列的引号+逗号完整还原");

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("A", "a,b"));
        rows.add(List.of("B", ""));
        rows.add(List.of("C", "\"x\""));
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            sb.append(Csv.formatRecord(row)).append('\n');
        }
        String[] lines = sb.toString().split("\n");
        t.check(lines.length == 3, "三行");
        boolean allOk = true;
        for (int i = 0; i < rows.size(); i++) {
            if (!Csv.parseLine(lines[i]).equals(rows.get(i))) {
                allOk = false;
            }
        }
        t.check(allOk, "★ 多行记录逐行 round-trip 全部成立");
    }
}

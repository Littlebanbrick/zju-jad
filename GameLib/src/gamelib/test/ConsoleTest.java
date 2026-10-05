package gamelib.test;

import gamelib.ui.Console;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Console 的测试。
 *
 * <p>每个用例用一段脚本文本喂进去，所以校验重试、EOF、默认答案这些
 * 「只在人工点菜单时才会遇到」的分支都能自动测。
 *
 * <p>两个重点：
 * <ul>
 *   <li>{@code readInt} 之后 {@code readLine} 不能拿到空串（不能用 {@code nextInt()}）</li>
 *   <li>EOF 必须抛 {@code CancelledException} 而不是死循环 —— 死循环会让测试挂住</li>
 * </ul>
 */
public final class ConsoleTest {

    private ConsoleTest() {
    }

    public static void run(TestRunner t) {
        testOutput(t);
        testReadLine(t);
        testReadInt(t);
        testReadDouble(t);
        testReadDate(t);
        testReadDateWithFallback(t);
        testReadYesNo(t);
        testConfirm(t);
        testEof(t);
        testNoLeftoverNewline(t);
    }

    // ---------- 输出 ----------

    private static void testOutput(TestRunner t) {
        t.section("[42] Console：输出方法");

        Fixture f = new Fixture("");
        f.console.print("a");
        f.console.print("b");
        t.check(f.output().equals("ab"), "print 不换行 -> [" + f.output() + "]");

        f.console.println("中文笔记，DLC别买");
        t.check(f.output().endsWith("ab中文笔记，DLC别买" + System.lineSeparator()),
                "println 用 UTF-8，中文不乱码 -> [" + f.output().replace("\n", "\\n") + "]");

        String before = f.output();
        f.console.println();
        t.check(f.output().length() > before.length(), "无参 println 输出一个换行");
    }

    // ---------- readLine / readRequired ----------

    private static void testReadLine(TestRunner t) {
        t.section("[43] Console：readLine / readRequired");

        Fixture f = new Fixture("\n非空值\n   带空白   \n");
        t.check(f.console.readLine("笔记: ").isEmpty(), "readLine 允许空串（笔记这类可选字段）");
        t.check(f.console.readRequired("标题: ").equals("非空值"), "readRequired 返回内容");
        t.check(f.console.readRequired("标题: ").equals("带空白"),
                "★ readRequired 去掉首尾空白");

        Fixture blank = new Fixture("   \n");
        expectCancelled(t, "★ 全空格也算留空 -> CancelledException",
                () -> blank.console.readRequired("标题: "));

        Fixture empty = new Fixture("\n");
        expectCancelled(t, "★ 空回车 -> CancelledException",
                () -> empty.console.readRequired("标题: "));
    }

    // ---------- readInt ----------

    private static void testReadInt(TestRunner t) {
        t.section("[44] Console：readInt 校验重试");

        Fixture ok = new Fixture("5\n");
        t.check(ok.console.readInt("评分: ", 0, 10) == 5, "正常输入 5 -> 5");

        Fixture spaces = new Fixture("  5  \n");
        t.check(spaces.console.readInt("评分: ", 0, 10) == 5, "★ 首尾空白被剥掉");

        Fixture bad = new Fixture("abc\n5\n");
        t.check(bad.console.readInt("评分: ", 0, 10) == 5, "★ 非整数后重问，最终返回 5");
        t.check(bad.output().contains("【警告】"), "打出了警告");
        t.check(bad.output().contains("abc"), "警告里提到了原输入");

        Fixture tooBig = new Fixture("11\n10\n");
        t.check(tooBig.console.readInt("评分: ", 0, 10) == 10, "★ 大于 max 后重问");
        t.check(tooBig.output().contains("0 到 10"), "警告里写了合法区间");

        Fixture tooSmall = new Fixture("-1\n0\n");
        t.check(tooSmall.console.readInt("评分: ", 0, 10) == 0, "★ 小于 min 后重问");

        Fixture min = new Fixture("0\n");
        t.check(min.console.readInt("评分: ", 0, 10) == 0, "★ 下界 0 合法");
        Fixture max = new Fixture("10\n");
        t.check(max.console.readInt("评分: ", 0, 10) == 10, "★ 上界 10 合法");

        Fixture cancelled = new Fixture("\n");
        expectCancelled(t, "留空 -> CancelledException",
                () -> cancelled.console.readInt("评分: ", 0, 10));
    }

    // ---------- readDouble ----------

    private static void testReadDouble(TestRunner t) {
        t.section("[45] Console：readDouble 与 NaN");

        Fixture ok = new Fixture("3.5\n");
        t.check(ok.console.readDouble("价格: ", 0, 1000) == 3.5, "正常 3.5");

        Fixture zero = new Fixture("0\n");
        t.check(zero.console.readDouble("价格: ", 0, 1000) == 0.0, "★ 0 元（Epic 白嫖）合法");

        Fixture bad = new Fixture("abc\n2.0\n");
        t.check(bad.console.readDouble("价格: ", 0, 1000) == 2.0, "非数字后重问");

        // 关键：parseDouble("NaN") 会成功返回 NaN，而 NaN 与任何数比较都是 false，
        // 能一路穿过区间检查
        Fixture nan = new Fixture("NaN\n1.5\n");
        t.check(nan.console.readDouble("价格: ", 0, 1000) == 1.5,
                "★ NaN 必须被挡住并重问（否则会穿过区间检查）");
        t.check(nan.output().contains("【警告】"), "NaN 打出了警告");

        // Infinity 由区间上界挡住
        Fixture infinity = new Fixture("1e999\n1.5\n");
        t.check(infinity.console.readDouble("价格: ", 0, 1000) == 1.5,
                "★ Infinity 被上界挡住并重问");

        Fixture over = new Fixture("-1\n0\n");
        t.check(over.console.readDouble("价格: ", 0, 1000) == 0.0, "负数被下界挡住");

        Fixture cancelled = new Fixture("\n");
        expectCancelled(t, "留空 -> CancelledException",
                () -> cancelled.console.readDouble("价格: ", 0, 1000));
    }

    // ---------- readDate ----------

    private static void testReadDate(TestRunner t) {
        t.section("[46] Console：readDate");

        Fixture ok = new Fixture("2022-03-01\n");
        t.check(ok.console.readDate("日期: ").equals(LocalDate.of(2022, 3, 1)), "正常日期");

        LocalDate today = LocalDate.now();
        Fixture todayFix = new Fixture(today + "\n");
        t.check(todayFix.console.readDate("日期: ").equals(today), "★ 今天合法");

        Fixture slash = new Fixture("2022/03/01\n2022-03-01\n");
        t.check(slash.console.readDate("日期: ").equals(LocalDate.of(2022, 3, 1)),
                "★ 斜杠格式被拒后重问");
        t.check(slash.output().contains("yyyy-MM-dd"), "警告里写了正确格式");

        Fixture month13 = new Fixture("2022-13-01\n2022-03-01\n");
        t.check(month13.console.readDate("日期: ").equals(LocalDate.of(2022, 3, 1)),
                "★ 13 月被拒后重问");

        Fixture future = new Fixture(today.plusDays(1) + "\n" + today + "\n");
        t.check(future.console.readDate("日期: ").equals(today),
                "★ 未来日期被拒后重问");
        t.check(future.output().contains("不能是未来"), "警告说明了原因");

        Fixture cancelled = new Fixture("\n");
        expectCancelled(t, "留空 -> CancelledException",
                () -> cancelled.console.readDate("日期: "));
    }

    // ---------- readDate(prompt, fallback) ----------

    private static void testReadDateWithFallback(TestRunner t) {
        t.section("[46b] Console：readDate(prompt, fallback)");

        LocalDate fallback = LocalDate.now();
        LocalDate past = LocalDate.of(2024, 3, 15);

        Fixture blank = new Fixture("\n");
        t.check(blank.console.readDate("通关日期: ", fallback).equals(fallback),
                "★ 留空 -> 返回 fallback（不是取消）");

        Fixture given = new Fixture("2024-03-15\n");
        t.check(given.console.readDate("通关日期: ", fallback).equals(past),
                "★ 给了日期就用给的（补录历史）");

        Fixture bad = new Fixture("2024/03/15\n2024-03-15\n");
        t.check(bad.console.readDate("通关日期: ", fallback).equals(past),
                "★ 格式错仍然重问，不走 fallback");
        t.check(bad.output().contains("【警告】"), "打出了警告");

        Fixture future = new Fixture(LocalDate.now().plusDays(1) + "\n" + past + "\n");
        t.check(future.console.readDate("通关日期: ", fallback).equals(past),
                "★ 未来日期仍然重问");

        // fallback == null 时退化成"留空即取消"，等同单参版本
        Fixture noFallback = new Fixture("\n");
        expectCancelled(t, "fallback 为 null 时留空 -> CancelledException",
                () -> noFallback.console.readDate("通关日期: ", null));

        // 单参版本不受影响
        Fixture single = new Fixture("2024-03-15\n");
        t.check(single.console.readDate("日期: ").equals(past), "单参版本行为不变");
    }

    // ---------- readYesNo ----------

    private static void testReadYesNo(TestRunner t) {
        t.section("[47] Console：readYesNo");

        t.check(new Fixture("y\n").console.readYesNo("已通关? ") , "\"y\" -> true");
        t.check(new Fixture("Y\n").console.readYesNo("已通关? "), "★ \"Y\" 大小写不敏感");
        t.check(new Fixture("yes\n").console.readYesNo("已通关? "), "\"yes\" -> true");
        t.check(new Fixture("是\n").console.readYesNo("已通关? "), "★ \"是\" -> true");
        t.check(!new Fixture("n\n").console.readYesNo("已通关? "), "\"n\" -> false");
        t.check(!new Fixture("N\n").console.readYesNo("已通关? "), "\"N\" -> false");
        t.check(!new Fixture("no\n").console.readYesNo("已通关? "), "\"no\" -> false");
        t.check(!new Fixture("否\n").console.readYesNo("已通关? "), "\"否\" -> false");

        Fixture retry = new Fixture("maybe\ny\n");
        t.check(retry.console.readYesNo("已通关? "), "★ 无法识别时重问");
        t.check(retry.output().contains("y 或 n"), "提示了合法输入");

        Fixture cancelled = new Fixture("\n");
        expectCancelled(t, "留空 -> CancelledException",
                () -> cancelled.console.readYesNo("已通关? "));
    }

    // ---------- confirm ----------

    private static void testConfirm(TestRunner t) {
        t.section("[48] Console：confirm（§4.4 删除确认）");

        t.check(new Fixture("y\n").console.confirm("确认删除?"), "\"y\" -> true");
        t.check(new Fixture("Y\n").console.confirm("确认删除?"), "\"Y\" -> true");
        t.check(new Fixture("是\n").console.confirm("确认删除?"), "\"是\" -> true");
        t.check(!new Fixture("n\n").console.confirm("确认删除?"), "\"n\" -> false");

        t.check(!new Fixture("\n").console.confirm("确认删除?"),
                "★ 直接回车 -> false（危险操作默认放弃）");

        t.check(!new Fixture("maybe\n").console.confirm("确认删除?"),
                "★ 乱输入 -> false，不重问也不抛异常");

        Fixture prompt = new Fixture("y\n");
        prompt.console.confirm("确认删除?");
        t.check(prompt.output().contains("[y/N]"), "★ 提示里标出默认值 [y/N]");

        // EOF 时 confirm 返回 false 而不是抛异常 —— 危险操作不该因为输入结束就执行
        t.check(!new Fixture("").console.confirm("确认删除?"),
                "★ EOF -> false（不抛异常，绝不默认删除）");
    }

    // ---------- EOF ----------

    private static void testEof(TestRunner t) {
        t.section("[49] Console：EOF 处理");

        Fixture empty = new Fixture("");
        t.check(!empty.console.hasMore(), "空脚本 hasMore() 为 false");

        Fixture some = new Fixture("1\n");
        t.check(some.console.hasMore(), "有内容时 hasMore() 为 true");

        // 若 EOF 不抛异常，下面的重试循环会疯狂打印警告，测试会挂住
        Fixture eofInt = new Fixture("");
        expectCancelled(t, "★ readInt 遇 EOF -> CancelledException（不死循环）",
                () -> eofInt.console.readInt("评分: ", 0, 10));

        Fixture eofLine = new Fixture("");
        expectCancelled(t, "★ readRequired 遇 EOF -> CancelledException",
                () -> eofLine.console.readRequired("标题: "));

        // 用了一半脚本再 EOF
        Fixture half = new Fixture("abc\n");
        expectCancelled(t, "★ 非法输入后遇 EOF 也要能跳出",
                () -> half.console.readInt("评分: ", 0, 10));
    }

    // ---------- 换行残留 ----------

    private static void testNoLeftoverNewline(TestRunner t) {
        t.section("[50] Console：nextInt 的换行残留（回归）");

        // 用 nextInt() 的话，换行符会留在缓冲里，下一次 readLine 会立刻返回空串
        Fixture f = new Fixture("42\nhello\n");
        t.check(f.console.readInt("n: ", 0, 100) == 42, "读到 42");
        t.check(f.console.readLine("s: ").equals("hello"),
                "★ 紧接着的 readLine 拿到 \"hello\"，不是空串");

        Fixture mixed = new Fixture("Steam\n298\n2022-03-01\n是\n神作\n");
        t.check(mixed.console.readRequired("平台: ").equals("Steam"), "第 1 个字段");
        t.check(mixed.console.readDouble("价格: ", 0, 10000) == 298.0, "第 2 个字段");
        t.check(mixed.console.readDate("日期: ").equals(LocalDate.of(2022, 3, 1)), "第 3 个字段");
        t.check(mixed.console.readYesNo("通关? "), "第 4 个字段");
        t.check(mixed.console.readLine("笔记: ").equals("神作"),
                "★ 混用五种读法，字段一个都没错位");
    }

    // ---------- 工具 ----------

    /** 一个 Console + 它写入的缓冲区。脚本用 {@code \n} 分隔每一行输入。 */
    private static final class Fixture {

        private final Console console;
        private final ByteArrayOutputStream buffer;

        Fixture(String script) {
            this.buffer = new ByteArrayOutputStream();
            this.console = new Console(
                    new Scanner(new StringReader(script)),
                    new PrintStream(buffer, true, StandardCharsets.UTF_8));
        }

        String output() {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private static void expectCancelled(TestRunner t, String what, Runnable action) {
        try {
            action.run();
            t.check(false, what + " —— 应当抛 CancelledException，但没有");
        } catch (Console.CancelledException e) {
            t.check(true, what + " -> " + e.getMessage());
        } catch (Throwable e) {
            t.check(false, what + " —— 抛了非预期异常："
                    + e.getClass().getSimpleName() + "：" + e.getMessage());
        }
    }
}

package gamelib.ui;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Scanner;

/**
 * 控制台输入输出原语 + 校验重试。
 *
 * <p>{@link Menu} 只调本类，不直接碰 {@code System.in} / {@code System.out}。
 * 流由构造器注入，所以整个菜单能用一段脚本文本驱动测试：
 * <pre>
 * var out = new ByteArrayOutputStream();
 * var input = new Scanner(new StringReader("1\nSteam\nElden Ring\n..."));
 * new Menu(lib, report, files,
 *          new Console(input, new PrintStream(out, true, StandardCharsets.UTF_8))).run();
 * </pre>
 *
 * <p><b>校验边界由调用方传入</b>，本类不 import {@code Game} ——
 * 评分范围要用 {@code Game.MIN_RATING} / {@code Game.MAX_RATING}，别写字面量。
 *
 * <p><b>三条铁律</b>：非法输入不退出程序（打提示再问）；校验方法不向外抛异常
 * （除了取消）；只 catch 具体的解析异常，绝不 {@code catch (Exception)}。
 */
public final class Console {

    private final Scanner in;
    private final PrintStream out;

    public Console(Scanner in, PrintStream out) {
        this.in = Objects.requireNonNull(in, "in 不能为 null");
        this.out = Objects.requireNonNull(out, "out 不能为 null");
    }

    // ---------- 输出 ----------

    public void print(String s) {
        out.print(s);
    }

    public void println(String s) {
        out.println(s);
    }

    public void println() {
        out.println();
    }

    // ---------- 输入 ----------

    /** 还有没有下一行。{@link Menu} 必须在打印提示<b>之前</b>检查，否则退出时会多留一行 {@code > }。 */
    public boolean hasMore() {
        return in.hasNextLine();
    }

    /**
     * 打印提示并读一行，返回原始内容（可能为空串）。笔记这类可选字段用它。
     *
     * <p>用 {@code nextLine()} 而不是 {@code nextInt()}：后者只吃掉数字，
     * 把换行符留在缓冲里，下一次读会立刻拿到空串。
     */
    public String readLine(String prompt) {
        print(prompt);
        return rawLine();
    }

    /**
     * 读一个非空值，首尾空白被去掉。
     *
     * @throws CancelledException 用户留空（= 取消本次操作），或输入流结束
     */
    public String readRequired(String prompt) {
        String raw = readLine(prompt).strip();
        if (raw.isEmpty()) {
            throw new CancelledException("必填字段留空，已取消本次操作");
        }
        return raw;
    }

    // ---------- 校验输入（内部重试，不向外抛） ----------

    /**
     * 读一个整数，必须在 {@code [min, max]} 闭区间内。越界或格式错都会重问。
     *
     * @throws CancelledException 留空或 EOF
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String raw = readRequired(prompt);
            try {
                int value = Integer.parseInt(raw);
                if (value < min || value > max) {
                    println("【警告】请输入 " + min + " 到 " + max + " 之间的整数，当前：" + value);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                println("【警告】'" + raw + "' 不是整数，请重新输入。");
            }
        }
    }

    /**
     * 读一个浮点数，必须在 {@code [min, max]} 闭区间内。
     *
     * <p>显式挡掉 {@code NaN}：{@code Double.parseDouble("NaN")} 会成功返回 NaN，
     * 而 NaN 和任何数比较都是 false，能一路穿过区间检查。
     *
     * @throws CancelledException 留空或 EOF
     */
    public double readDouble(String prompt, double min, double max) {
        while (true) {
            String raw = readRequired(prompt);
            try {
                double value = Double.parseDouble(raw);
                if (Double.isNaN(value)) {
                    println("【警告】'" + raw + "' 不是有效数字，请重新输入。");
                    continue;
                }
                if (value < min || value > max) {
                    println("【警告】请输入 " + min + " 到 " + max + " 之间的数字，当前：" + value);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                println("【警告】'" + raw + "' 不是数字，请重新输入。");
            }
        }
    }

    /**
     * 读一个日期，格式必须是 {@code yyyy-MM-dd}，且不能晚于今天。
     *
     * <p>「不能是未来」和 {@code Game.requireNotFuture} 是<b>故意重复</b>的：
     * 这里发现非法能只重问这一个字段，而让异常穿到 {@code new Game(...)}
     * 就只能把整张表单重填一遍。
     *
     * @throws CancelledException 留空或 EOF
     */
    public LocalDate readDate(String prompt) {
        LocalDate today = LocalDate.now();
        while (true) {
            String raw = readRequired(prompt);
            try {
                LocalDate date = LocalDate.parse(raw);
                if (date.isAfter(today)) {
                    println("【警告】日期不能是未来：" + date + "（今天是 " + today + "）");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                println("【警告】日期格式应为 yyyy-MM-dd，例如 2022-03-01。收到：'" + raw + "'");
            }
        }
    }

    /**
     * 读一个是/否。给「是否已通关」这类非破坏性问题用。
     *
     * <p><b>用 {@code nextLine()} 自己判断，不要用 {@code Boolean.parseBoolean}</b> ——
     * 它只认 "true"，其他一律返回 false 且不报错，于是 "maybe" 会静默变成 false。
     *
     * @throws CancelledException 留空或 EOF
     */
    public boolean readYesNo(String prompt) {
        while (true) {
            String raw = readRequired(prompt).toLowerCase();
            if (raw.equals("y") || raw.equals("yes") || raw.equals("是")) {
                return true;
            }
            if (raw.equals("n") || raw.equals("no") || raw.equals("否")) {
                return false;
            }
            println("【警告】请输入 y 或 n。");
        }
    }

    /**
     * 危险操作的确认（§4.4 删除）。<b>直接回车等于放弃</b>。
     *
     * <p>和 {@link #readYesNo} 的差别：这里不重问、不抛异常，
     * 任何非肯定输入（含空串和 EOF）都返回 {@code false} —— 默认走安全分支。
     */
    public boolean confirm(String prompt) {
        print(prompt + " [y/N] ");
        if (!in.hasNextLine()) {
            return false;
        }

        String raw = in.nextLine().strip().toLowerCase();
        return raw.equals("y") || raw.equals("yes") || raw.equals("是");
    }

    // ---------- 内部 ----------

    /** EOF 必须能跳出，否则重试循环会疯狂打印警告。 */
    private String rawLine() {
        if (!in.hasNextLine()) {
            throw new CancelledException("输入已结束");
        }
        return in.nextLine();
    }

    /**
     * 用户在必填字段留空，或输入流结束。
     *
     * <p>不是错误，是<b>控制流</b>：Menu 在表单外面 catch 一次，
     * 就能让任何字段随时退出。用返回值表达的话，每个字段后面都要写
     * {@code if (x.isEmpty()) return;} —— 八个字段就是八处，漏一处就出错。
     */
    public static class CancelledException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public CancelledException(String message) {
            super(message);
        }
    }
}

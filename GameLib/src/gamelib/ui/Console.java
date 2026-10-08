package gamelib.ui;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Scanner;

public final class Console {

    private final Scanner in;
    private final PrintStream out;

    public Console(Scanner in, PrintStream out) {
        this.in = Objects.requireNonNull(in, "in 不能为 null");
        this.out = Objects.requireNonNull(out, "out 不能为 null");
    }

    public void print(String s) {
        out.print(s);
    }

    public void println(String s) {
        out.println(s);
    }

    public void println() {
        out.println();
    }

    public boolean hasMore() {
        return in.hasNextLine();
    }

    public String readLine(String prompt) {
        print(prompt);
        return rawLine();
    }

    public String readRequired(String prompt) {
        String raw = readLine(prompt).strip();
        if (raw.isEmpty()) {
            throw new CancelledException("必填字段留空，已取消本次操作");
        }
        return raw;
    }

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

    public LocalDate readDate(String prompt) {
        return readDate(prompt, null);
    }

    public LocalDate readDate(String prompt, LocalDate fallback) {
        LocalDate today = LocalDate.now();
        while (true) {
            String raw = (fallback == null) ? readRequired(prompt) : readLine(prompt).strip();
            if (raw.isEmpty()) {
                return fallback;
            }
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

    public boolean confirm(String prompt) {
        print(prompt + " [y/N] ");
        if (!in.hasNextLine()) {
            return false;
        }

        String raw = in.nextLine().strip().toLowerCase();
        return raw.equals("y") || raw.equals("yes") || raw.equals("是");
    }

    private String rawLine() {
        if (!in.hasNextLine()) {
            throw new CancelledException("输入已结束");
        }
        return in.nextLine();
    }

    public static class CancelledException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public CancelledException(String message) {
            super(message);
        }
    }
}

package gamelib;

import gamelib.io.LibraryFiles;
import gamelib.io.LoadReport;
import gamelib.store.GameLibrary;
import gamelib.ui.Console;
import gamelib.ui.Menu;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 程序入口：只做接线，不含任何业务逻辑。
 *
 * <p>测试入口是 {@code gamelib.test.SelfTest}，不是这里。
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // 显式 UTF-8：两参数的 PrintStream(OutputStream) 用平台默认编码，
        // Windows 上中文会乱码
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);

        LibraryFiles files = LibraryFiles.defaults();

        // §4.6 启动加载：先二进制，再 CSV，都没有才新建。
        // 文件存在却读不出来时 load() 会抛异常 —— 绝不静默降级成空库，
        // 否则用户以为数据没了，一保存就把旧存档覆盖掉。
        LoadReport report;
        try {
            report = files.load();
        } catch (RuntimeException e) {
            out.println("【错误】游戏库加载失败：" + e.getMessage());
            out.println("  程序中止，以免用空库覆盖掉你的存档。");
            return;
        }

        GameLibrary lib = new GameLibrary();
        lib.addAll(report.games());

        new Menu(lib, report, files, new Console(in, out)).run();
    }
}

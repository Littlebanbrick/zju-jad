package gamelib;

import gamelib.io.LibraryFiles;
import gamelib.io.LoadReport;
import gamelib.store.GameLibrary;
import gamelib.ui.Console;
import gamelib.ui.Menu;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {

        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);

        LibraryFiles files = LibraryFiles.defaults();

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

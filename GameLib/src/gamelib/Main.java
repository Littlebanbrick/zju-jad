package gamelib;

import gamelib.io.LibraryFiles;
import gamelib.io.LoadReport;
import gamelib.store.GameLibrary;
import gamelib.store.ImportReport;
import gamelib.ui.Console;
import gamelib.ui.Menu;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
        ImportReport imported = lib.addAll(report.games());

        // 存档里如果有重复行（手工编辑过、或把两次导出合并了），addAll 会静默跳过。
        // 把这件事并进 warnings，让 Menu 的启动提示一并打出来 —— 否则用户看到的
        // 行数和实际入库数不一致，而且没有任何说明。
        if (imported.duplicates() > 0 || imported.skipped() > 0) {
            List<String> warnings = new ArrayList<>(report.warnings());
            if (imported.duplicates() > 0) {
                warnings.add("存档里有 " + imported.duplicates() + " 条重复被跳过："
                        + String.join("、", imported.duplicateTitles()));
            }
            if (imported.skipped() > 0) {
                warnings.add("存档里有 " + imported.skipped() + " 条无效记录被忽略。");
            }
            report = new LoadReport(report.source(), report.games(), warnings);
        }

        new Menu(lib, report, files, new Console(in, out)).run();
    }
}

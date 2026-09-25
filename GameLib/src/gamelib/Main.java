package gamelib;

import java.util.Locale;
import java.util.Scanner;

/**
 * 程序入口：控制台菜单。
 *
 * <p>目前是骨架 —— 只有「退出」真正可用，其余分支在第 2–4 步逐个接上。
 * 测试不在这个类里，跑 {@code java -cp out gamelib.test.SelfTest}。
 */
public class Main {

    private static final String MENU = """
            ===================================================
             主菜单
             1) 添加游戏
             2) 查看全部
             3) 搜索游戏
             4) 更新游戏
             5) 删除游戏
             6) 统计报表
             7) 保存并退出
            ---------------------------------------------------
             快速命令： add / search <关键词> / stats / exit
            ===================================================""";

    public static void main(String[] args) {
        System.out.println("========== Personal Game Library Manager ==========");
        System.out.println("（菜单骨架：功能将在第 2–4 步接上）");

        try (Scanner in = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                System.out.println(MENU);
                System.out.print("> ");
                if (!in.hasNextLine()) {
                    break;                  // Ctrl-D：当作退出
                }
                running = handle(in.nextLine());
            }
        }

        System.out.println("感谢使用，再见");
    }

    /**
     * 处理一行输入。命令词与参数以空白分隔，这样 §7 的快速命令
     * {@code search witch} 也能被正确拆开。
     *
     * @return false 表示应当退出主循环
     */
    private static boolean handle(String line) {
        String[] parts = line.trim().split("\\s+", 2);
        String verb = parts[0].toLowerCase(Locale.ROOT);
        String arg = (parts.length > 1) ? parts[1] : "";

        switch (verb) {
            case "1", "add" -> todo(2, "添加游戏");
            case "2", "list" -> todo(2, "查看全部");
            case "3", "search" -> todo(2, "搜索游戏" + (arg.isEmpty() ? "" : "：" + arg));
            case "4", "update" -> todo(2, "更新游戏");
            case "5", "delete" -> todo(3, "删除游戏");
            case "6", "stats" -> todo(3, "统计报表");
            case "7", "exit", "quit" -> {
                return false;
            }
            case "" -> {
                // 直接回车：不做事，重新显示菜单
            }
            default -> System.out.println("【警告】未知命令：" + verb + "（输入 7 退出）");
        }
        return true;
    }

    private static void todo(int step, String name) {
        System.out.println("【待实现】" + name + " —— 第 " + step + " 步接上");
    }
}

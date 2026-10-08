package gamelib.test;

import gamelib.model.InvalidGameDataException;

public final class TestRunner {

    private int passed = 0;
    private int failed = 0;

    public void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    public void check(boolean ok, String what) {
        if (ok) {
            passed++;
            System.out.println("  [PASS] " + what);
        } else {
            failed++;
            System.out.println("  [FAIL] " + what);
        }
    }

    public void expectInvalid(String what, Runnable action) {
        expectThrows(InvalidGameDataException.class, what, action);
    }

    public void expectThrows(Class<? extends Throwable> expected, String what, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  [FAIL] " + what
                    + " —— 应当抛 " + expected.getSimpleName() + "，但没有");
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                passed++;
                System.out.println("  [PASS] " + what + " -> " + actual.getMessage());
            } else {
                failed++;
                System.out.println("  [FAIL] " + what
                        + " —— 期望 " + expected.getSimpleName()
                        + "，实际抛了 " + actual.getClass().getSimpleName()
                        + "：" + actual.getMessage());
            }
        }
    }

    public int passed() {
        return passed;
    }

    public int failed() {
        return failed;
    }

    public void crash(String suite, Throwable e) {
        failed++;
        System.out.println();
        System.out.println("  [CRASH] " + suite + " 抛出未捕获异常："
                + e.getClass().getSimpleName() + "：" + e.getMessage());
        for (StackTraceElement frame : e.getStackTrace()) {
            if (frame.getClassName().startsWith("gamelib.")) {
                System.out.println("          at " + frame);
            }
        }
    }

    public void printSummary() {
        System.out.println();
        System.out.println("===================================");
        System.out.printf("通过 %d 项，失败 %d 项%n", passed, failed);
        if (failed == 0) {
            System.out.println("Game 契约冻结，测试通过。");
        }
    }
}

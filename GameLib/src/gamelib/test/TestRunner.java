package gamelib.test;

import gamelib.model.InvalidGameDataException;

/**
 * 手写测试工具。
 *
 * <p>每个测试类都通过 {@code run(TestRunner t)} 接收同一个实例，
 * 由 SelfTest 汇总统计。不搞全局静态计数器，避免隐式共享状态。
 */
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

    /**
     * 断言 action 必须抛 InvalidGameDataException（含子类，如 DuplicateGameException），
     * 并打印消息供肉眼检查可读性。
     */
    public void expectInvalid(String what, Runnable action) {
        expectThrows(InvalidGameDataException.class, what, action);
    }

    /**
     * 断言 action 抛出指定类型的异常（含子类）。
     *
     * <p>非预期类型记为 FAIL，而不是让它炸穿整个测试流程 ——
     * 一个坏用例不该阻止后面所有用例执行。
     */
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

    /**
     * 测试套件自身崩了（未捕获异常）。记为 1 个 FAIL，不影响其他套件继续跑 ——
     * 否则第一个套件的崩溃会掩盖后面所有套件的结果。
     */
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

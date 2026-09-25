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
     * 断言 action 必须抛 InvalidGameDataException，并打印消息供肉眼检查可读性。
     *
     * <p>非预期异常（如 NPE）记为 FAIL
     */
    public void expectInvalid(String what, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  [FAIL] " + what + " —— 应当抛异常，但没有");
        } catch (InvalidGameDataException e) {
            passed++;
            System.out.println("  [PASS] " + what + " -> " + e.getMessage());
        } catch (RuntimeException e) {
            failed++;
            System.out.println("  [FAIL] " + what + " —— 抛了非预期异常：" + e);
        }
    }

    public int passed() {
        return passed;
    }

    public int failed() {
        return failed;
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

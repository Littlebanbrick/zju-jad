package gamelib.test;

/**
 * 测试入口。不是程序入口 —— 程序入口是 {@link gamelib.Main}（菜单）。
 *
 * <p>运行：
 * <pre>
 *   ./test.sh
 *   java -cp out gamelib.test.SelfTest
 * </pre>
 *
 * <p>加了新测试类，只在这里补一行 {@code XxxTest.run(t);}。
 */
public final class SelfTest {

    private SelfTest() {
    }

    public static void main(String[] args) {
        TestRunner t = new TestRunner();

        System.out.println("========== Game 冒烟测试 ==========");

        PlatformTest.run(t);
        GameConstructTest.run(t);
        GameEqualsTest.run(t);
        GameEqualsTest.runStorePreview(t);
        GameStateTest.run(t);

        t.printSummary();

        if (t.failed() > 0) {
            System.exit(1);
        }
    }
}

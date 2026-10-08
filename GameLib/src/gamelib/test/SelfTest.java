package gamelib.test;

public final class SelfTest {

    private SelfTest() {
    }

    public static void main(String[] args) {
        TestRunner t = new TestRunner();

        System.out.println("========== Game 冒烟测试 ==========");

        runSafely(t, "PlatformTest", () -> PlatformTest.run(t));
        runSafely(t, "GameConstructTest", () -> GameConstructTest.run(t));
        runSafely(t, "GameEqualsTest", () -> GameEqualsTest.run(t));
        runSafely(t, "GameEqualsTest.runStorePreview", () -> GameEqualsTest.runStorePreview(t));
        runSafely(t, "GameStateTest", () -> GameStateTest.run(t));
        runSafely(t, "GameLibraryTest", () -> GameLibraryTest.run(t));
        runSafely(t, "CsvTest", () -> CsvTest.run(t));
        runSafely(t, "GameCsvTest", () -> GameCsvTest.run(t));
        runSafely(t, "StatsTest", () -> StatsTest.run(t));
        runSafely(t, "GameFormatterTest", () -> GameFormatterTest.run(t));
        runSafely(t, "ConsoleTest", () -> ConsoleTest.run(t));
        runSafely(t, "MenuTest", () -> MenuTest.run(t));

        t.printSummary();

        if (t.failed() > 0) {
            System.exit(1);
        }
    }

    private static void runSafely(TestRunner t, String name, Runnable suite) {
        try {
            suite.run();
        } catch (Throwable e) {
            t.crash(name, e);
        }
    }
}

package gamelib.ui;

import gamelib.model.Game;
import gamelib.model.Platform;
import gamelib.store.GameLibrary;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record Stats(
        int totalGames,
        double totalSpent,
        double averageRating,
        Map<Platform, Integer> countByPlatform,
        Map<Platform, Double> spendByPlatform,
        int completedCount,
        Map<Integer, Integer> completedByYear) 
{
    public static Stats of(GameLibrary lib) {
        Objects.requireNonNull(lib, "lib 不能为 null");
        List<Game> games = lib.all();

        Map<Platform, Integer> counts = new TreeMap<>();
        Map<Platform, Double> spends = new TreeMap<>();
        Map<Integer, Integer> completedByYear = new TreeMap<>();

        double totalSpent = 0;
        int ratingSum = 0;
        int completedCount = 0;

        for (Game g: games) {
            totalSpent += g.getPurchasePrice();
            ratingSum += g.getRating();

            Platform p = g.getPlatform();
            counts.put(p, counts.getOrDefault(p, 0) + 1);
            spends.put(p, spends.getOrDefault(p, 0.0) + g.getPurchasePrice());

            LocalDate done = g.getCompletedDate();
            if (done != null) {     // 未通关的不进年度统计
                completedCount++;
                int year = done.getYear();
                completedByYear.put(year, completedByYear.getOrDefault(year, 0) + 1);
            }
        }

        // 丢掉 0 款的平台
        Map<Platform, Integer> countByPlatform = new LinkedHashMap<>();
        Map<Platform, Double> spendByPlatform = new LinkedHashMap<>();
        for (Platform p: Platform.values()) {
            if (counts.containsKey(p)) {
                countByPlatform.put(p, counts.get(p));
                spendByPlatform.put(p, spends.get(p));
            }
        }

        double averageRating = games.isEmpty() ? 0.0 : (double) ratingSum / games.size();

        return new Stats(games.size(), 
                        totalSpent, 
                        averageRating,
                        countByPlatform, 
                        spendByPlatform, 
                        completedCount, 
                        completedByYear
            );
    }

    public int unfinishedCount() {
        return totalGames - completedCount;
    }

    public boolean isEmpty() {
        return totalGames == 0;
    }

    // 并列时取第一个
    public Optional<Platform> mostExpensivePlatform() {
        Platform result = null;
        double max = -1;        // 不能是0！否则只有免费游戏时返回null
        for (Map.Entry<Platform, Double> e: spendByPlatform.entrySet()) {
            if (e.getValue() > max) {       // 严格大于
                max = e.getValue();
                result = e.getKey();
            }
        }
        return Optional.ofNullable(result);
    }
}
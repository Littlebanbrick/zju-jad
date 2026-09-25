package gamelib.store;

import gamelib.model.Game;
import gamelib.model.Platform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.Set;
import java.util.HashSet;

public class GameLibrary {

    /**
     * 唯一的存储：key = {@link Game#key()}（规范化标题 + 平台），value = Game 本体。
     *
     * <p>LinkedHashMap 而不是 HashMap：保持入库顺序，「查看全部」的输出才稳定可复现。
     *
     * <p>按平台分组、总数、List&lt;Game&gt; 全是现算的派生数据，不另存字段 ——
     * 存了就得在每次 add/remove 时同步维护，漏一处就永久不一致，而且只在
     * 某次特定操作之后才显形。
     */
    private final Map<String, Game> byKey = new LinkedHashMap<>();

    /** 新建一个空库。 */
    public GameLibrary() {}

    public void add(Game game) {
        Objects.requireNonNull(game, "game 不能为 null");
        String gameKey = game.key();
        if(!byKey.containsKey(gameKey)) {
            // DNE
            byKey.put(gameKey, game);
        } else {
            throw new DuplicateGameException(
                "该游戏已存在！请勿重复入库。 " + 
                game.getTitle() + " | " +
                game.getPlatform()
            );
        }
    }

    public Optional<Game> remove(String title, Platform platform) {
        String key = Game.keyOf(title, platform);
        if(!byKey.containsKey(key)) {
            // DNE
            return Optional.empty();
        } else {
            Game game = byKey.get(key);
            byKey.remove(key);
            return Optional.of(game);
        }
    }

    public Optional<Game> find(String title, Platform platform) {
        String key = Game.keyOf(title, platform);
        if(!byKey.containsKey(key)) {
            // DNE
            return Optional.empty();
        } else {
            return Optional.of(byKey.get(key));
        }
    }

    public boolean contains(String title, Platform platform) {
        return byKey.containsKey(Game.keyOf(title, platform));
    }

    public int size() {
        return byKey.size();
    }

    public boolean isEmpty() {
        return byKey.isEmpty();
    }

    public List<Game> all() {
        List<Game> list = new ArrayList<>();
        for(Game game : byKey.values()) {
            if(!game.isHidden()) {
                list.add(game);
            }
        }
        return list;
    }

    public List<Game> allIncludingHidden() {
        return new ArrayList<>(byKey.values());
    }

    private List<Game> matching(Predicate<Game> p) {
        List<Game> result = new ArrayList<>();
        for(Game game : byKey.values()) {
            if(p.test(game)) {
                result.add(game);
            }
        }
        return result;
    }

    // 别忘了 filter 自动排除 hidden 游戏
    public List<Game> filter(Predicate<Game> p) {
        return matching(g -> !g.isHidden() && p.test(g));
    }

    // 模糊搜索
    public List<Game> searchByTitle(String keyword) {
        String lowerK = keyword.trim().toLowerCase();
        return filter(g -> g.getTitle().toLowerCase().contains(lowerK));
    }

    public List<Game> searchByPlatform(Platform platform) {
        return filter(g -> g.getPlatform() == platform);
    }

    // 模糊搜索
    public List<Game> searchByGenre(String genre) {
        String lowerG = genre.trim().toLowerCase();
        return filter(g -> g.getGenre().toLowerCase().contains(lowerG));
    }

    public List<Game> searchByCompleted(boolean completed) {
        return filter(g -> g.isCompleted() == completed);
    }

    public List<Game> searchByPriceRange(double min, double max) {
        return filter(g -> g.getPurchasePrice() >= min 
                        && g.getPurchasePrice() <= max);
    }

    public Map<Platform, List<Game>> groupByPlatform() {
        Map<Platform, List<Game>> result = new LinkedHashMap<>();

        for (Platform p : Platform.values()) {
            result.put(p, new ArrayList<>());
        }
        for (Game g : filter(x -> true)) {
            result.get(g.getPlatform()).add(g);
        }
        return result;
    }

    public Set<String> allGenres() {
        Set<String> genres = new HashSet<>();
        for(Game game : byKey.values()) {
            if(!game.isHidden()) {
                genres.add(game.getGenre());
            }
        }
        return genres;
    }
}

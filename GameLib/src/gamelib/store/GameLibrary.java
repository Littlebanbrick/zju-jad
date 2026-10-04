package gamelib.store;

import gamelib.model.Game;
import gamelib.model.Platform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collection;
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

    /**
     * 单条入库（§4.1）。重复时抛异常 —— 交互式录入需要用户立刻知道。
     *
     * @throws DuplicateGameException 标题 + 平台已存在
     */
    public void add(Game game) {
        if (!tryAdd(game)) {
            throw new DuplicateGameException("该游戏已存在！请勿重复入库。 "
                    + game.getTitle() + " | " + game.getPlatform());
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

    public ImportReport addAll(Collection<Game> games) {
        int added = 0;
        int duplicates = 0;
        int skipped = 0;
        List<String> duplicateTitles = new ArrayList<>();

        for (Game g : games) {
            if (g == null) {
                skipped++;
                continue;
            }
            if (tryAdd(g)) {
                added++;
            } else {
                duplicates++;
                duplicateTitles.add(g.getTitle() + " | " + g.getPlatform());
            }
        }
        return new ImportReport(added, duplicates, skipped, duplicateTitles);
    }

    /**
     * 真正的插入。{@code true} = 加进去了，{@code false} = 已存在。
     *
     * <p>唯一性规则只在这一处，{@code add} 和 {@code addAll} 共用 ——
     * 否则日后改规则要改两遍。故意 private：对外只暴露两种语义
     * （{@code add} 抛异常、{@code addAll} 跳过），不给第三种"静默返回 false"。
     *
     * <p>{@code putIfAbsent} 一行同时做到插入和判重，且已存在时不动原值 ——
     * 这正是"重复导入不覆盖库里那份"的由来。
     */
    private boolean tryAdd(Game game) {
        Objects.requireNonNull(game, "game 不能为 null");
        return byKey.putIfAbsent(game.key(), game) == null;
    }
}

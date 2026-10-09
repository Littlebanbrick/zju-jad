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

    // 注意，final 只是保证引用不可变，引用指向的对象本身仍然可以修改
    // 比如 byKey.put(..., ...) 是合法的，
    // 但 byKey = new LinkedHashMap<>() 是不合法的
    private final Map<String, Game> byKey = new LinkedHashMap<>();

    // 前面 byKey 是 final，就地初始化，构造器无事可做
    public GameLibrary() {}

    public void add(Game game) {
        if (!tryAdd(game)) {
            throw new DuplicateGameException("该游戏已存在！请勿重复入库。 "
                    + game.getTitle() + " | " + game.getPlatform());
        }
    }

    public Optional<Game> remove(String title, Platform platform) {
        String key = Game.keyOf(title, platform);
        if(!byKey.containsKey(key)) {
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

        // Collection 是 java 中的一个接口，List、Set、Queue 等都实现了 Collection 接口，Map 则没有
        // 对于 Map，values() 方法返回一个 Collection，包含所有的 value
        for(Game game: byKey.values()) {
            if(!game.isHidden()) {
                list.add(game);
            }
        }
        return list;
    }

    public List<Game> allIncludingHidden() {
        // 看到没？可以直接用构造器把 Collection 转成 List
        return new ArrayList<>(byKey.values());
    }

    // Predicate n.谓词
    // Predicate 是一个函数式接口，只有一个抽象方法 test(T t)，返回 boolean
    // 这里的 p 是一个函数式接口，传入一个 Game，返回 boolean
    // 基本语法： 参数 -> boolean表达式
    // 调用示例：
    //      Predicate<Game> p = g -> g.getRating() >= 4;
    //      List<Game> goodGames = library.filter(p);
    // 或直接写作：
    //      List<Game> goodGames = library.filter(g -> g.getRating() >= 4);
    // 表示筛选出评分大于等于4的游戏
    // p.test(g) 方法返回 true/false，表示 g 是否符合条件
    private List<Game> matching(Predicate<Game> p) {
        List<Game> result = new ArrayList<>();
        for(Game game : byKey.values()) {
            if(p.test(game)) {
                result.add(game);
            }
        }
        return result;
    }

    public List<Game> filter(Predicate<Game> p) {
        return matching(g -> !g.isHidden() && p.test(g));
    }

    public List<Game> searchByTitle(String keyword) {
        String lowerK = keyword.trim().toLowerCase();
        return filter(g -> g.getTitle().toLowerCase().contains(lowerK));
    }

    public List<Game> searchByPlatform(Platform platform) {
        return filter(g -> g.getPlatform() == platform);
    }

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

        for (Platform p: Platform.values()) {
            result.put(p, new ArrayList<>());
        }

        // filter(x -> true) 表示不过滤，返回所有游戏
        for (Game g: filter(x -> true)) {
            result.get(g.getPlatform()).add(g);
        }
        return result;
    }

    // 为什么用 Set 而不是 List？因为 Set 不允许重复元素，而我们只想要所有不同的 genre
    public Set<String> allGenres() {
        Set<String> genres = new HashSet<>();
        for(Game game: byKey.values()) {
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

        for (Game g: games) {
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

    private boolean tryAdd(Game game) {
        Objects.requireNonNull(game, "game 不能为 null");

        // putIfAbsent() 方法会在 key 不存在时才插入，并返回 null
        // 如果 key 已存在，返回已存在的值
        return byKey.putIfAbsent(game.key(), game) == null;
    }
}

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

    private final Map<String, Game> byKey = new LinkedHashMap<>();

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

    private boolean tryAdd(Game game) {
        Objects.requireNonNull(game, "game 不能为 null");
        return byKey.putIfAbsent(game.key(), game) == null;
    }
}

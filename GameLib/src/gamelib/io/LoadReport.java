package gamelib.io;

import gamelib.model.Game;

import java.util.List;

public record LoadReport(Source source, List<Game> games, List<String> warnings) {

    public enum Source {

        BINARY,

        CSV,

        EMPTY
    }

    public int gameCount() {
        return games.size();
    }

    public LoadReport {
        games = List.copyOf(games);
        warnings = List.copyOf(warnings);
    }
}

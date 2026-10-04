package gamelib.io;

import gamelib.model.Game;

import java.util.List;

/**
 * 启动加载的结果。
 *
 * @param source   数据来自哪里
 * @param games    加载到的游戏（{@code EMPTY} 时为空列表）
 * @param warnings 降级过程中的说明，供 UI 打印
 */
public record LoadReport(Source source, List<Game> games, List<String> warnings) {

    /** 数据来源。 */
    public enum Source {
        /** 二进制存档 game_lib.dat */
        BINARY,
        /** 文本 game_lib.csv */
        CSV,
        /** 两个文件都不存在，§4.6 的「新建游戏库」 */
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
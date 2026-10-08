package gamelib.io;

import gamelib.io.LoadReport.Source;
import gamelib.model.Game;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LibraryFiles {

    private final Path csvPath;
    private final Path binaryPath;
    private final Path backupPath;

    public LibraryFiles(Path csvPath, Path binaryPath, Path backupPath) {
        this.csvPath = Objects.requireNonNull(csvPath, "csvPath 不能为 null");
        this.binaryPath = Objects.requireNonNull(binaryPath, "binaryPath 不能为 null");
        this.backupPath = Objects.requireNonNull(backupPath, "backupPath 不能为 null");
    }

    public static LibraryFiles defaults() {
        return new LibraryFiles(
            Path.of("game_lib.csv"),
            Path.of("game_lib.dat"),
            Path.of("game_lib_backup.dat")
        );
    }

    public Path csvPath() {
        return csvPath;
    }

    public Path binaryPath() {
        return binaryPath;
    }

    public Path backupPath() {
        return backupPath;
    }

    public LoadReport load() {
        List<String> warnings = new ArrayList<>();

        boolean binaryMissing = !Files.exists(binaryPath);
        if (binaryMissing) {
            warnings.add("二进制存档 '" + binaryPath + "' 不存在。");
        } else {
            try {
                List<Game> games = readBinary();
                warnings.add("已从二进制存档 '" + binaryPath + "' 加载 " + games.size() + " 款游戏。");
                return new LoadReport(Source.BINARY, games, warnings);
            } catch (RuntimeException e) {
                warnings.add("二进制存档 '" + binaryPath + "' 损坏，回退到 CSV：" + e.getMessage());
            }
        }

        boolean csvMissing = !Files.exists(csvPath);
        if (csvMissing) {
            warnings.add("文本存档 '" + csvPath + "' 不存在。");
        } else {
            try {
                List<Game> games = readCsv();
                warnings.add("已从 '" + csvPath + "' 加载 " + games.size() + " 款游戏。");
                return new LoadReport(Source.CSV, games, warnings);
            } catch (RuntimeException e) {
                throw new UncheckedIOException(
                        "两个存档都读不出来，拒绝以空库启动（否则一保存就覆盖旧数据）：" + warnings,
                        new IOException(e));
            }
        }

        if (binaryMissing && csvMissing) {
            warnings.add("未找到任何存档，将新建游戏库。");
            return new LoadReport(Source.EMPTY, List.of(), warnings);
        }

        throw new UncheckedIOException(
                "存档存在但读不出来，拒绝以空库启动（否则一保存就覆盖旧数据）：" + warnings,
                new IOException(warnings.toString()));
    }

    public void saveBinary(List<Game> games) {
        Objects.requireNonNull(games, "games 不能为 null");
        try {

            if (Files.exists(binaryPath)) {
                Files.copy(binaryPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            Path tmp = binaryPath.resolveSibling(binaryPath.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(new ArrayList<>(games));
            }

            Files.move(tmp, binaryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("保存二进制存档失败：" + binaryPath, e);
        }
    }

    public void saveCsv(List<Game> games) {
        Objects.requireNonNull(games, "games 不能为 null");
        try {
            Files.writeString(csvPath, GameCsv.toCsvText(games), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("导出 CSV 失败：" + csvPath, e);
        }
    }

    public List<Game> importCsv(Path source) {
        Objects.requireNonNull(source, "source 不能为 null");
        try {
            return GameCsv.parseCsvText(Files.readString(source, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("读取 CSV 失败：" + source, e);
        }
    }

    private List<Game> readCsv() {
        return importCsv(csvPath);
    }

    private List<Game> readBinary() {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(binaryPath))) {
            Object raw = in.readObject();
            if (!(raw instanceof List<?> list)) {
                throw corrupt("存档内容不是列表，实际是 "
                        + raw.getClass().getName() + "：" + binaryPath);
            }
            List<Game> games = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof Game g)) {
                    throw corrupt("存档里混进了非 Game 对象："
                            + element.getClass().getName() + "：" + binaryPath);
                }
                games.add(g);
            }
            return games;

        } catch (ClassNotFoundException e) {

            throw new UncheckedIOException("存档里的类不存在（编译版本不同？）：" + binaryPath,
                    new IOException(e));
        } catch (IOException e) {

            throw new UncheckedIOException("读取二进制存档失败：" + binaryPath, e);
        }
    }

    private static UncheckedIOException corrupt(String message) {
        return new UncheckedIOException(message, new IOException(message));
    }
}

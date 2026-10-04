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

/**
 * 唯一碰磁盘的类（§4.6）。
 *
 * <p>{@link Csv} 管语法、{@link GameCsv} 管 schema，两个都只处理字符串。
 * 本类只做搬运：文件 &lt;-&gt; 字符串 &lt;-&gt; {@code List<Game>}。
 *
 * <p><b>路径注入而不是写死</b>：测试要写到临时目录，不能污染工作目录，
 * 所以路径由构造器传入，{@link #defaults()} 才是生产用的三个文件名。
 */
public final class LibraryFiles {

    private final Path csvPath;
    private final Path binaryPath;
    private final Path backupPath;

    public LibraryFiles(Path csvPath, Path binaryPath, Path backupPath) {
        this.csvPath = Objects.requireNonNull(csvPath, "csvPath 不能为 null");
        this.binaryPath = Objects.requireNonNull(binaryPath, "binaryPath 不能为 null");
        this.backupPath = Objects.requireNonNull(backupPath, "backupPath 不能为 null");
    }

    /** 生产用：当前工作目录下的三个文件名（§4.6）。 */
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

    // ---------- 加载 ----------

    /**
     * 启动加载：先二进制（快），再文本，都没有才新建库（§4.6）。
     *
     * <p><b>不静默降级。</b>文件存在却读不出来时抛异常，不返回空库 ——
     * 否则用户以为数据没了，一保存就把旧存档覆盖成空的。
     */
    public LoadReport load() {
        List<String> warnings = new ArrayList<>();

        // 1. 二进制存档：正常路径 
        boolean binaryMissing = !Files.exists(binaryPath);
        if (binaryMissing) {
            warnings.add("二进制存档 '" + binaryPath + "' 不存在。");
        } else {
            try {
                return new LoadReport(Source.BINARY, readBinary(), warnings);
            } catch (RuntimeException e) {
                warnings.add("二进制存档 '" + binaryPath + "' 损坏，回退到 CSV：" + e.getMessage());
            }
        }

        // 2. 文本存档：回退路径
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

        // 3. 真的都没有 -> §4.6 的「新建游戏库」
        if (binaryMissing && csvMissing) {
            warnings.add("未找到任何存档，将新建游戏库。");
            return new LoadReport(Source.EMPTY, List.of(), warnings);
        }

        // 二进制存在但坏了，且没有 CSV 兜底
        throw new UncheckedIOException(
                "存档存在但读不出来，拒绝以空库启动（否则一保存就覆盖旧数据）：" + warnings,
                new IOException(warnings.toString()));
    }

    // ---------- 保存 ----------

    /**
     * 保存二进制存档（§4.6）。顺序固定：先备份，再写临时文件，最后原子替换。
     *
     * <p><b>不能直接写 {@code game_lib.dat}</b>
     */
    public void saveBinary(List<Game> games) {
        Objects.requireNonNull(games, "games 不能为 null");
        try {
            // 1. 备份
            if (Files.exists(binaryPath)) {
                Files.copy(binaryPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 2. 写临时文件
            Path tmp = binaryPath.resolveSibling(binaryPath.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(new ArrayList<>(games));
            }

            // 3. 原子替换
            Files.move(tmp, binaryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("保存二进制存档失败：" + binaryPath, e);
        }
    }

    /**
     * 导出 CSV（§4.6 的「可被 Excel 打开」）。
     *
     * <p>{@link GameCsv#toCsvText} 已经带上 BOM，所以这里不用额外处理。
     * 故意不做备份：{@code game_lib_backup.dat} 备份的是二进制存档，CSV 是导出格式。
     */
    public void saveCsv(List<Game> games) {
        Objects.requireNonNull(games, "games 不能为 null");
        try {
            Files.writeString(csvPath, GameCsv.toCsvText(games), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("导出 CSV 失败：" + csvPath, e);
        }
    }

    // ---------- 批量导入 ----------

    /**
     * §4.1 的批量导入：读任意路径的 CSV（不是存档位置），返回待入库的游戏。
     *
     * <p>只读不改：查重和入库交给 {@code GameLibrary.addAll}。
     */
    public List<Game> importCsv(Path source) {
        Objects.requireNonNull(source, "source 不能为 null");
        try {
            return GameCsv.parseCsvText(Files.readString(source, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("读取 CSV 失败：" + source, e);
        }
    }

    // ---------- 内部 ----------

    private List<Game> readCsv() {
        return importCsv(csvPath);
    }

    /**
     * 读二进制存档。
     *
     * <p>逐个元素做 {@code instanceof} 检查
     * {@code ClassCastException}。
     */
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
            // readObject 的签名里就有这个 checked 异常
            throw new UncheckedIOException("存档里的类不存在（编译版本不同？）：" + binaryPath,
                    new IOException(e));
        } catch (IOException e) {
            // 覆盖 InvalidClassException（serialVersionUID 不匹配）和 EOFException（文件被截断）
            throw new UncheckedIOException("读取二进制存档失败：" + binaryPath, e);
        }
    }

    /**
     * 文件读得出来但内容不对。
     *
     * <p>统一用 {@link UncheckedIOException}，这样 ui 层只需要 catch 一种异常类型
     * 就能覆盖所有持久化失败。
     */
    private static UncheckedIOException corrupt(String message) {
        return new UncheckedIOException(message, new IOException(message));
    }
}

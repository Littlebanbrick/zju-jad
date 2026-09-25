package gamelib.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Game implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 10;

    /**
     * key() 由这两个字段算出，hence final。
     */
    private final String title;
    private final Platform platform;

    private final String genre;
    private final double purchasePrice;     // >=0
    private final LocalDate purchaseDate;   // <= 今天
    private LocalDate completedDate;    // nullable, where null means uncompleted
    private double playtimeHours;
    private int rating;
    private String notes;
    private boolean hidden;

    public Game(
        String title,
        Platform platform,
        String genre,
        double purchasePrice,
        LocalDate purchaseDate,
        int rating,
        double playtimeHours,
        String note
    ) {
        this.title = requireNonBlank(title, "标题");
        this.platform = requireNonNull(platform, "平台");
        this.genre = requireNonBlank(genre, "类型");
        this.purchasePrice = requireNonNegative(purchasePrice, "购买价格");
        this.purchaseDate = requireNotFuture(purchaseDate, "购买日期");
        this.rating = requireInRange(rating, MIN_RATING, MAX_RATING, "个人评分");
        this.playtimeHours = requireNonNegative(playtimeHours, "游玩时长");
        this.notes = (note == null) ? "" : note.trim();
        this.completedDate = null;   // null = 未通关
        this.hidden = false;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidGameDataException(field + "不能为空");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new InvalidGameDataException(field + "不能为空");
        }
        return value;
    }

    private static double requireNonNegative(double value, String field) {
        if (value < 0) {
            throw new InvalidGameDataException(field + "不能为负：" + value);
        }
        return value;
    }

    private static int requireInRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new InvalidGameDataException(
                field + "必须在 " + min + "–" + max + " 之间：" + value);
        }
        return value;
    }

    private static LocalDate requireNotFuture(LocalDate value, String field) {
        if (value == null) {
            throw new InvalidGameDataException(field + "不能为空");
        }
        if (value.isAfter(LocalDate.now())) {
            throw new InvalidGameDataException(field + "不能是未来日期：" + value);
        }
        return value;
    }

    public String getTitle() {
        return this.title;
    }

    public Platform getPlatform() {
        return this.platform;
    }

    public String getGenre() {
        return this.genre;
    }

    public double getPurchasePrice() {
        return this.purchasePrice;
    }

    public LocalDate getPurchaseDate() {
        return this.purchaseDate;
    }

    public int getRating() {
        return this.rating;
    }

    public double getPlaytimeHours() {
        return this.playtimeHours;
    }

    public String getNotes() {
        return this.notes;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public boolean isCompleted() {
        return completedDate != null;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void addPlaytime(double hours) {
        this.playtimeHours += requireNonNegative(hours, "新增游玩时长");
    }

    public void markCompleted() {
        if(this.completedDate == null) {
            this.completedDate = LocalDate.now();
        }
    }

    public void markCompleted(LocalDate completedDate) {
        LocalDate date = requireNotFuture(completedDate, "通关日期");
        // 通关日期不能早于购买日期
        if(this.purchaseDate.isAfter(date)) {
            throw new InvalidGameDataException
                ("通关日期" + completedDate + "不能早于购买日期" + this.purchaseDate);
        }

        this.completedDate = date;
    }

    public void updateRating(int rating) {
        this.rating = requireInRange(rating, MIN_RATING, MAX_RATING, "个人评分");
    }

    public void setNotes(String notes) {
        this.notes = (notes == null) ? "" : notes.trim();
    }

    public void hide() {
        this.hidden = true;
    }

    public void unhide() {
        this.hidden = false;
    }

    // ---------- 契约方法 ----------

    /**
     * 唯一键：规范化标题 + 平台。查重、equals、hashCode 全部经由这里。
     * key 只由两个 final 字段算出，所以对象放进 HashMap 不会失联。
     */
    public String key() {
        return keyOf(title, platform);
    }

    public static String keyOf(String title, Platform platform) {
        return normalizeKey(requireNonBlank(title, "标题"))
             + "|" + requireNonNull(platform, "平台").name();
    }

    /** 大小写不敏感。 */
    private static String normalizeKey(String title) {
        return title.trim().toLowerCase();
    }

    /** "同一款游戏" = key 相同。不看价格、时长、评分、隐藏状态。 */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Game other)) {   // Java 16+ 模式匹配
            return false;
        }
        return key().equals(other.key());
    }

    /** 必须与 equals 成对：equals 相等时 hashCode 必须相等，否则 HashMap 查不到。 */
    @Override
    public int hashCode() {
        return key().hashCode();
    }

    /** 仅调试用。对齐表格属于 ui 层，不写在这里。 */
    @Override
    public String toString() {
        return "Game[" + key()
             + ", price=" + purchasePrice
             + ", playtime=" + playtimeHours + "hour(s)"
             + ", rating=" + rating
             + (isCompleted() ? ", completed=" + completedDate : ", not completed")
             + "]";
    }
}

package gamelib.model;

import java.io.Serializable;
import java.time.LocalDate;

// .dat不是一种格式，意思是“二进制数据，具体内容程序自己定义”
// 如果 Game 不实现 Serializable 接口，就会因为无法序列化而无法写入.dat文件
// 同时，Game 所有字段也必须实现 Serializable
public class Game implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 10;

    private final String title;
    private final Platform platform;

    private final String genre;
    private final double purchasePrice;

    // 关于 LocalDate：
    // 只有日期，没有时间和时区。实例不可变（和String类似）
    // 例如：LocalDate.of(2023, 5, 1) 表示 2023-05-01
    // LocalDate.now() 基于操作系统时间返回当前日期，无需联网
    // LocalDate.toString() 返回 ISO-8601 格式的字符串，例如 "2023-05-01"
    // LocalDate.parse("2023-05-01") 将 ISO-8601 格式的字符串解析为 LocalDate
    // 比较必须用方法，比如 isBefore()、isAfter()、isEqual()
    // 可以为null
    // 有getMonth()、getDayOfMonth()等方法获取年月日
    // getMonthValue = 3; getMonth = MARCH; getDayOfMonth = 15; getDayOfWeek = WEDNESDAY
    private final LocalDate purchaseDate;
    private LocalDate completedDate;
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
        this.completedDate = null;
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

    public String key() {
        return keyOf(title, platform);
    }

    public static String keyOf(String title, Platform platform) {
        return normalizeKey(requireNonBlank(title, "标题"))
             + "|" + requireNonNull(platform, "平台").name();
    }

    private static String normalizeKey(String title) {
        return title.trim().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        // instanceof的两种形态：
        // 1. object instanceof ClassName
        //    返回 true/false，判断 object 是否是 ClassName 的实例
        // 2. object instanceof ClassName variableName
        //    如果 object 是 ClassName 的实例，则把 object 强制转换为 ClassName，
        //    并赋值给 variableName，返回 true；否则返回 false
        //
        // 这里的写法等价于：
        // if (!(o instanceof Game)) {
        //     return false;
        // }
        // Game other = (Game) o;
        //
        // 问题来了：为什么 o 已经是 Game 的实例了，还要再强制转换为 Game？
        // 因为 o 声明的类型是 Object，Object 没有 key() 方法（编译器只看声明，不看运行时），
        // 所以必须先转换为 Game 才能调用 key() 方法
        if (!(o instanceof Game other)) {
            return false;
        }
        return key().equals(other.key());
    }

    @Override
    // hashCode() 原本用于返回对象的哈希值
    // 这里 hashCode 应与 equals() 保持一致，
    //      即如果两个对象 equals() 返回 true，则它们的 hashCode() 必须相等
    // 故只与 key() 相关
    public int hashCode() {
        return key().hashCode();
    }

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

package gamelib.model;

/**
 * 数据违反了 Game / Platform 的业务规则（负价格、未来日期、评分越界等）。
 *
 * <p>继承 RuntimeException（unchecked）：非法数据只会在两个边界被捕获
 * —— UI 输入层和后面的 CSV 导入层 —— 中间层不必到处写 throws 声明。
 *
 * <p>不提供无参构造器：没有 message 的校验异常对用户无用。
 */
public class InvalidGameDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidGameDataException(String message) {
        super(message);     // RuntimeException(String)
    }

    /** 包装底层异常（如 CSV 里的 NumberFormatException），保留原始 cause。 */
    public InvalidGameDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
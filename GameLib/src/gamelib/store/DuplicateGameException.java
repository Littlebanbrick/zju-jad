package gamelib.store;

import gamelib.model.InvalidGameDataException;

// 区分“数据不合法”和“数据重复”
// 暂时没有单独的用处，可供后续扩展
public class DuplicateGameException extends InvalidGameDataException {

    private static final long serialVersionUID = 1L;

    public DuplicateGameException(String message) {
        super(message);
    }

    public DuplicateGameException(String message, Throwable cause) {
        super(message, cause);
    }
}

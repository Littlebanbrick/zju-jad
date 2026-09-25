package gamelib.store;

import gamelib.model.InvalidGameDataException;

public class DuplicateGameException extends InvalidGameDataException {

    private static final long serialVersionUID = 1L;

    public DuplicateGameException(String message) {
        super(message);
    }

    public DuplicateGameException(String message, Throwable cause) {
        super(message, cause);
    }
}

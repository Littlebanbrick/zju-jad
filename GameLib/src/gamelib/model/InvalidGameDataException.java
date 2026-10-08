package gamelib.model;

public class InvalidGameDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidGameDataException(String message) {
        super(message);
    }

    public InvalidGameDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

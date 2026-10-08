package gamelib.model;

public enum Platform {

    STEAM("Steam"),
    EPIC("Epic"),
    GOG("GOG"),
    XBOX("Xbox"),
    OTHER("Other");

    private final String label;

    Platform(String label) {
        this.label = label;
    }

    public static Platform fromString(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidGameDataException("平台不能为空，可选：" + choices());
        }
        String s = input.trim().toUpperCase();
        for (Platform p: values()) {
            if (p.name().equals(s) || p.label.toUpperCase().equals(s)) {
                return p;
            }
        }
        throw new InvalidGameDataException("未知平台：'" + input + "'，可选：" + choices());
    }

    public static String choices() {
        StringBuilder sb = new StringBuilder();
        for (Platform p: values()) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(p.label);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return label;
    }
}

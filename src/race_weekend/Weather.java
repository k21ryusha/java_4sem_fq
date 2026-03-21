package race_weekend;

public enum Weather {
    DRY("Сухо", 1.0),
    WET("Мокро", 0.5),
    RAIN("Дождь", 0.3);

    private final String title;
    private final double multiplier;

    Weather(String title, double multiplier) {
        this.title = title;
        this.multiplier = multiplier;
    }

    public String getTitle() {
        return title;
    }

    public double getMultiplier() {
        return multiplier;
    }
}

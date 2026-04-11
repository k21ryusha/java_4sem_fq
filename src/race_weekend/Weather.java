package race_weekend;

public enum Weather {
    DRY("Сухо", 1.0, "Ясно, трасса сухая"),
    WET("Мокро", 0.5, "Трасса влажная, сцепление падает"),
    RAIN("Дождь", 0.3, "Начинается дождь, болиду нужна адаптация"),
    SNOW("Снег", 0.18, "Пошел снег, пилотам приходится ехать осторожнее");

    private final String title;
    private final double multiplier;
    private final String raceMessage;

    Weather(String title, double multiplier, String raceMessage) {
        this.title = title;
        this.multiplier = multiplier;
        this.raceMessage = raceMessage;
    }

    public String getTitle() {
        return title;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public String getRaceMessage() {
        return raceMessage;
    }
}

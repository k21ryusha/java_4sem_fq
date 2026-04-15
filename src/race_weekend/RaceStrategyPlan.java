package race_weekend;

public enum RaceStrategyPlan {
    PRIMARY(
            "Основная",
            "Стабильный темп и ранний пит-стоп при износе",
            new RaceTactic("Базовая тактика", 0.985, 0.992, 1.000, 61.0, 0.018)
    ),
    ALTERNATIVE(
            "Альтернативная",
            "Риск в сухую погоду, но лучше адаптация к осадкам",
            new RaceTactic("Погодная тактика", 0.996, 0.978, 0.968, 68.0, 0.010)
    );

    private final String title;
    private final String description;
    private final RaceTactic tactic;

    RaceStrategyPlan(String title, String description, RaceTactic tactic) {
        this.title = title;
        this.description = description;
        this.tactic = tactic;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RaceTactic getTactic() {
        return tactic;
    }
}

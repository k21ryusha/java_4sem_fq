package race_weekend;

public class RaceTactic {
    private final String name;
    private final double dryModifier;
    private final double wetModifier;
    private final double rainModifier;
    private final double pitThreshold;
    private final double safetyMargin;

    public RaceTactic(String name,
                      double dryModifier,
                      double wetModifier,
                      double rainModifier,
                      double pitThreshold,
                      double safetyMargin) {
        this.name = name;
        this.dryModifier = dryModifier;
        this.wetModifier = wetModifier;
        this.rainModifier = rainModifier;
        this.pitThreshold = pitThreshold;
        this.safetyMargin = safetyMargin;
    }

    public String getName() {
        return name;
    }

    public double modifierFor(Weather weather) {
        return switch (weather) {
            case DRY -> dryModifier;
            case WET -> wetModifier;
            case RAIN -> rainModifier;
        };
    }

    public double getPitThreshold() {
        return pitThreshold;
    }

}

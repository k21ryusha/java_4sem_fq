package components;

public class Engine extends Component {
    private final EngineType type;
    private final int power;
    private final int mass;

    public Engine(String name, int price, int quality, EngineType type, int power, int mass) {
        super(name, price, quality);
        this.type = type;
        this.power = power;
        this.mass = mass;
    }

    public EngineType getType() {
        return type;
    }

    public int getPower() {
        return power;
    }

    public int getMass() {
        return mass;
    }
}

package components;

public class Transmission extends Component {
    private final EngineType compatibleType;
    private final int efficiency;

    public Transmission(String name, int price, int quality, EngineType compatibleType, int efficiency) {
        super(name, price, quality);
        this.compatibleType = compatibleType;
        this.efficiency = efficiency;
    }

    public EngineType getCompatibleType() {
        return compatibleType;
    }

    public int getEfficiency() {
        return efficiency;
    }
}

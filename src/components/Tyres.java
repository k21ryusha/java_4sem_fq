package components;

public class Tyres extends Component {
    private final String compound;
    private final int grip;
    private final int durability;
    private final int rainPerformance;

    public Tyres(String name, int price, int quality, String compound, int grip, int durability, int rainPerformance) {
        super(name, price, quality);
        this.compound = compound;
        this.grip = grip;
        this.durability = durability;
        this.rainPerformance = rainPerformance;
    }

    public String getCompound() {
        return compound;
    }

    public int getGrip() {
        return grip;
    }

    public int getDurability() {
        return durability;
    }

    public int getRainPerformance() {
        return rainPerformance;
    }
}

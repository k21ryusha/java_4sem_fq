package components;

public class Suspension extends Component {
    private final String compatibleClass;
    private final int cornerBonus;

    public Suspension(String name, int price, int quality, String compatibleClass, int cornerBonus) {
        super(name, price, quality);
        this.compatibleClass = compatibleClass;
        this.cornerBonus = cornerBonus;
    }

    public String getCompatibleClass() {
        return compatibleClass;
    }

    public int getCornerBonus() {
        return cornerBonus;
    }
}

package components;

public class Chassis extends Component {
    private final int maxEngineMass;
    private final String chassisClass;
    private final int handling;

    public Chassis(String name, int price, int quality, int maxEngineMass, String chassisClass, int handling) {
        super(name, price, quality);
        this.maxEngineMass = maxEngineMass;
        this.chassisClass = chassisClass;
        this.handling = handling;
    }

    public int getMaxEngineMass() {
        return maxEngineMass;
    }

    public String getChassisClass() {
        return chassisClass;
    }

    public int getHandling() {
        return handling;
    }
}

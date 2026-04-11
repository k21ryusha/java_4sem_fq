package weapons;

import java.io.Serial;
import java.io.Serializable;

public abstract class Weapon implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int maxCompatibleMass;
    private final double accuracy;

    protected Weapon(String name, int maxCompatibleMass, double accuracy) {
        this.name = name;
        this.maxCompatibleMass = maxCompatibleMass;
        this.accuracy = accuracy;
    }

    public String getName() {
        return name;
    }

    public int getMaxCompatibleMass() {
        return maxCompatibleMass;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public boolean supportsMass(int mass) {
        return mass <= maxCompatibleMass;
    }

    protected String describeType() {
        return "weapon";
    }

    @Override
    public String toString() {
        return name + " [" + describeType() + ", maxMass=" + maxCompatibleMass
                + ", accuracy=" + Math.round(accuracy * 100) + "%]";
    }
}

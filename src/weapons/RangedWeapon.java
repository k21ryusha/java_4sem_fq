package weapons;

public class RangedWeapon extends Weapon {
    public RangedWeapon(String name, int maxCompatibleMass, double accuracy) {
        super(name, maxCompatibleMass, accuracy);
    }

    @Override
    protected String describeType() {
        return "ranged";
    }
}

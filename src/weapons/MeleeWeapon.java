package weapons;

public class MeleeWeapon extends Weapon {
    public MeleeWeapon(String name, int maxCompatibleMass, double accuracy) {
        super(name, maxCompatibleMass, accuracy);
    }

    @Override
    protected String describeType() {
        return "melee";
    }
}

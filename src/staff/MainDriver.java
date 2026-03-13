package staff;

public class MainDriver extends FreeAgent {
    private final int cornerSkill;
    private final int straightSkill;
    private final int consistency;
    private final int wetSkill;

    public MainDriver(String name, int contractCost, int cornerSkill, int straightSkill, int consistency, int wetSkill) {
        super(name, contractCost);
        this.cornerSkill = cornerSkill;
        this.straightSkill = straightSkill;
        this.consistency = consistency;
        this.wetSkill = wetSkill;
    }

    public int getCornerSkill() { return cornerSkill; }
    public int getStraightSkill() { return straightSkill; }
    public int getConsistency() { return consistency; }
    public int getWetSkill() { return wetSkill; }

    @Override
    public String toString() {
        return name + " [corner=" + cornerSkill + ", straight=" + straightSkill + "]";
    }
}

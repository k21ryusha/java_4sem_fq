package staff;

public class MainDriver extends FreeAgent {
    public static final int MAX_DISCONTENT = 100;

    private final int cornerSkill;
    private final int straightSkill;
    private final int consistency;
    private final int wetSkill;
    private int discontent;

    public MainDriver(String name, int contractCost, int cornerSkill, int straightSkill, int consistency,
                      int wetSkill, int discontent) {
        super(name, contractCost);
        this.cornerSkill = cornerSkill;
        this.straightSkill = straightSkill;
        this.consistency = consistency;
        this.wetSkill = wetSkill;
        this.discontent = Math.max(0, Math.min(discontent, MAX_DISCONTENT - 1));
    }

    public int getCornerSkill() { return cornerSkill; }
    public int getStraightSkill() { return straightSkill; }
    public int getConsistency() { return consistency; }
    public int getWetSkill() { return wetSkill; }

    @Override
    public String toString() {
        return name + " [corner=" + cornerSkill + ", straight=" + straightSkill + "]";
    }

    public int getDiscontent() {
        return discontent;
    }

    public void addDiscontent(int value) {
        if (value <= 0) {
            return;
        }
        discontent += value;
        if (discontent >= MAX_DISCONTENT) {
            discontent = 0;
        }
    }
}

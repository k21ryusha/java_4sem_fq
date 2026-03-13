package race_weekend;

public class Track {
    private final String name;
    private final double lapKm;
    private final int laps;
    private final int corners;
    private final int straights;
    private final int elevation;

    public Track(String name, double lapKm, int laps, int corners, int straights, int elevation) {
        this.name = name;
        this.lapKm = lapKm;
        this.laps = laps;
        this.corners = corners;
        this.straights = straights;
        this.elevation = elevation;
    }

    public String getName() { return name; }
    public double getLapKm() { return lapKm; }
    public int getLaps() { return laps; }
    public int getCorners() { return corners; }
    public int getStraights() { return straights; }
    public int getElevation() { return elevation; }

    @Override
    public String toString() {
        return name + " (" + lapKm + " км, кругов: " + laps + ")";
    }
}
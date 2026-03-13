package race_weekend;

import java.util.List;

public class RaceResult {
    private final String trackName;
    private final Weather weather;
    private final List<String> table;

    public RaceResult(String trackName, Weather weather, List<String> table) {
        this.trackName = trackName;
        this.weather = weather;
        this.table = table;
    }

    public String getTrackName() {
        return trackName;
    }

    public Weather getWeather() {
        return weather;
    }

    public List<String> getTable() {
        return table;
    }
}

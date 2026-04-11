package save;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class SaveSlot {
    private final Path path;
    private final String displayName;
    private final boolean autoSave;
    private final LocalDateTime savedAt;

    public SaveSlot(Path path, String displayName, boolean autoSave, LocalDateTime savedAt) {
        this.path = path;
        this.displayName = displayName;
        this.autoSave = autoSave;
        this.savedAt = savedAt;
    }

    public Path getPath() {
        return path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}

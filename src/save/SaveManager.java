package save;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class SaveManager {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Path savesRoot;
    private final Gson gson;

    public SaveManager() {
        this(Path.of("saves"));
    }

    public SaveManager(Path savesRoot) {
        this.savesRoot = savesRoot;
        this.gson = GsonFactory.create();
    }

    public List<SaveSlot> listSaves(String playerName) {
        Path playerDir = resolvePlayerDirectory(playerName);
        if (!Files.exists(playerDir)) {
            return List.of();
        }

        List<SaveSlot> saves = new ArrayList<>();
        try (Stream<Path> paths = Files.list(playerDir)) {
            paths.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> saves.add(toSaveSlot(path)));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать список сохранений.", e);
        }

        saves.sort(Comparator.comparing(SaveSlot::getSavedAt).reversed());
        return saves;
    }

    public SaveSlot saveManual(GameState state) {
        return save(state, "manual");
    }

    public SaveSlot saveAuto(GameState state) {
        return save(state, "autosave");
    }

    public GameState load(SaveSlot slot) {
        try {
            String json = Files.readString(slot.getPath(), StandardCharsets.UTF_8);
            return gson.fromJson(json, GameState.class);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить сохранение " + slot.getDisplayName(), e);
        }
    }

    private SaveSlot save(GameState state, String prefix) {
        LocalDateTime now = LocalDateTime.now();
        Path playerDir = resolvePlayerDirectory(state.getPlayerName());
        try {
            Files.createDirectories(playerDir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать папку сохранений.", e);
        }

        String fileName = prefix + "_" + now.format(FILE_TIMESTAMP) + ".json";
        Path savePath = playerDir.resolve(fileName);
        try {
            Files.writeString(savePath, gson.toJson(state), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить игру.", e);
        }
        return toSaveSlot(savePath);
    }

    private SaveSlot toSaveSlot(Path path) {
        String fileName = path.getFileName().toString();
        boolean autoSave = fileName.startsWith("autosave_");
        LocalDateTime savedAt;
        try {
            savedAt = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            savedAt = LocalDateTime.now();
        }

        String saveType;
        if (autoSave) {
            saveType = "Автосохранение";
        } else {
            saveType = "Ручное сохранение";
        }
        String displayName = saveType + " [" + savedAt.format(DISPLAY_TIMESTAMP) + "]";
        return new SaveSlot(path, displayName, autoSave, savedAt);
    }

    private Path resolvePlayerDirectory(String playerName) {
        return savesRoot.resolve(sanitizeFileName(playerName));
    }

    private String sanitizeFileName(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-zа-я0-9_-]+", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        sanitized = sanitized.replaceAll("^_|_$", "");
        if (sanitized.isBlank()) {
            return "player";
        }
        return sanitized;
    }
}

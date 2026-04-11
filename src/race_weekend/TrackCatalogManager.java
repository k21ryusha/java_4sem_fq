package race_weekend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TrackCatalogManager {
    private static final Type TRACK_LIST_TYPE = new TypeToken<List<Track>>() { }.getType();

    private final Path catalogFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public TrackCatalogManager() {
        this(Path.of("tracks", "custom_tracks.xml"));
    }

    public TrackCatalogManager(Path catalogFile) {
        this.catalogFile = catalogFile;
    }

    public List<Track> loadCustomTracks() {
        Path source = resolveExistingCatalog();
        if (source == null) {
            return new ArrayList<>();
        }
        try {
            if (source.getFileName().toString().endsWith(".json")) {
                String json = Files.readString(source, StandardCharsets.UTF_8);
                List<Track> tracks = parseLegacyJsonTracks(json);
                if (tracks == null) {
                    return new ArrayList<>();
                }
                return new ArrayList<>(tracks);
            }
            return readTracksFromXml(source);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить пользовательские трассы.", e);
        }
    }

    public void saveNewTrack(Track track) {
        List<Track> tracks = loadCustomTracks();
        upsertTrack(tracks, null, track);
        writeTracks(tracks);
    }

    public void updateTrack(String originalName, Track updatedTrack) {
        List<Track> tracks = loadCustomTracks();
        upsertTrack(tracks, originalName, updatedTrack);
        writeTracks(tracks);
    }

    private void upsertTrack(List<Track> tracks, String originalName, Track updatedTrack) {
        String nameToReplace;
        if (originalName == null) {
            nameToReplace = updatedTrack.getName();
        } else {
            nameToReplace = originalName;
        }
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getName().equalsIgnoreCase(nameToReplace)) {
                tracks.set(i, updatedTrack);
                return;
            }
        }
        tracks.add(updatedTrack);
    }

    private void writeTracks(List<Track> tracks) {
        try {
            Files.createDirectories(catalogFile.getParent());
            writeTracksAsXml(tracks);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить пользовательские трассы.", e);
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IllegalStateException("Не удалось сохранить пользовательские трассы.", e);
        }
    }

    private Path resolveExistingCatalog() {
        if (Files.exists(catalogFile)) {
            return catalogFile;
        }
        Path legacyJsonFile = toLegacyJsonPath();
        if (Files.exists(legacyJsonFile)) {
            return legacyJsonFile;
        }
        return null;
    }

    private Path toLegacyJsonPath() {
        String fileName = catalogFile.getFileName().toString();
        if (fileName.endsWith(".xml")) {
            return catalogFile.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".json");
        }
        return catalogFile;
    }

    private List<Track> parseLegacyJsonTracks(String json) {
        try {
            return gson.fromJson(json, TRACK_LIST_TYPE);
        } catch (RuntimeException originalException) {
            String normalized = json.trim();
            if (normalized.startsWith("{")) {
                return gson.fromJson("[" + normalized, TRACK_LIST_TYPE);
            }
            throw originalException;
        }
    }

    private List<Track> readTracksFromXml(Path source) throws IOException {
        try {
            byte[] xmlBytes = Files.readAllBytes(source);
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlBytes));
            NodeList nodes = document.getElementsByTagName("track");
            List<Track> tracks = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element trackElement = (Element) nodes.item(i);
                tracks.add(new Track(
                        readText(trackElement, "name"),
                        Double.parseDouble(readText(trackElement, "lapKm")),
                        Integer.parseInt(readText(trackElement, "laps")),
                        Integer.parseInt(readText(trackElement, "corners")),
                        Integer.parseInt(readText(trackElement, "straights")),
                        Integer.parseInt(readText(trackElement, "elevation"))
                ));
            }
            return tracks;
        } catch (Exception e) {
            throw new IOException("Ошибка чтения XML каталога трасс.", e);
        }
    }

    private void writeTracksAsXml(List<Track> tracks) throws ParserConfigurationException, TransformerException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("tracks");
        document.appendChild(root);

        for (Track track : tracks) {
            Element trackElement = document.createElement("track");
            root.appendChild(trackElement);
            appendTextElement(document, trackElement, "name", track.getName());
            appendTextElement(document, trackElement, "lapKm", String.valueOf(track.getLapKm()));
            appendTextElement(document, trackElement, "laps", String.valueOf(track.getLaps()));
            appendTextElement(document, trackElement, "corners", String.valueOf(track.getCorners()));
            appendTextElement(document, trackElement, "straights", String.valueOf(track.getStraights()));
            appendTextElement(document, trackElement, "elevation", String.valueOf(track.getElevation()));
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        try (BufferedWriter writer = Files.newBufferedWriter(catalogFile, StandardCharsets.UTF_8)) {
            transformer.transform(new DOMSource(document), new StreamResult(writer));
        } catch (IOException e) {
            throw new TransformerException("Не удалось записать XML каталог трасс.", e);
        }
    }

    private void appendTextElement(Document document, Element parent, String tagName, String value) {
        Element element = document.createElement(tagName);
        element.appendChild(document.createTextNode(value));
        parent.appendChild(element);
    }

    private String readText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new IllegalStateException("В XML отсутствует поле " + tagName);
        }
        return nodes.item(0).getTextContent();
    }
}

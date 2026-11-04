package com.github.blade.mappeek;

import org.bukkit.World;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The wind as moving particles on squaremap (0.2.0, opt-in with
 * {@code wind.animation}). squaremap has no way for addons to add to its
 * page, so MapPeek copies its own script into squaremap's web folder and adds
 * one {@code <script>} line to its {@code index.html}, again whenever
 * squaremap writes a fresh page. Each wind refresh writes the wind on its grid
 * for the script. Switched off, all of it is taken away again.
 */
final class WindFlow {
    private WindFlow() {}

    static final String FOLDER = "mappeek";
    static final String SCRIPT = FOLDER + "/wind.js";
    static final String TAG = "<script src=\"" + SCRIPT + "\"></script>";

    /** squaremap's page, with MapPeek's script before squaremap's own so it can catch the map. */
    static String inject(String page) {
        if (page.contains(TAG)) return page;
        int head = page.indexOf("<head>");
        if (head < 0) return page;
        int at = head + "<head>".length();
        return page.substring(0, at) + "\n        " + TAG + page.substring(at);
    }

    static String remove(String page) {
        return page.replace("\n        " + TAG, "").replace(TAG, "");
    }

    /**
     * The wind on its grid for the script: each point's x and z, its wind
     * toward the east and the south in m/s, and its level (0 calm .. 3 gale);
     * and the height to draw it at on a 3D map, sea level.
     */
    static String field(List<WindArrow> arrows, int spacing, String[] colors, int y) {
        StringBuilder json = new StringBuilder(64 + arrows.size() * 32);
        json.append("{\"spacing\":").append(spacing).append(",\"y\":").append(y).append(",\"colors\":[");
        for (int i = 0; i < colors.length; i++) json.append(i > 0 ? "," : "").append('"').append(colors[i]).append('"');
        json.append("],\"points\":[");
        boolean first = true;
        for (WindArrow arrow : arrows) {
            double toward = Math.toRadians(arrow.towardDegrees());
            double speed = Double.isFinite(toward) ? arrow.speedMs() : 0.0;
            double east = Double.isFinite(toward) ? Math.sin(toward) * speed : 0.0;
            double south = Double.isFinite(toward) ? -Math.cos(toward) * speed : 0.0;
            json.append(first ? "" : ",").append(String.format(Locale.US, "[%.0f,%.0f,%.2f,%.2f,%d]",
                    arrow.x(), arrow.z(), hundredths(east), hundredths(south), arrow.level().ordinal()));
            first = false;
        }
        return json.append("]}").toString();
    }

    /** Rounded to hundredths, with no "-0.00". */
    private static double hundredths(double value) {
        return Math.round(value * 100.0) / 100.0 + 0.0;
    }

    /** Each level's colour for the script, calm first. */
    static String[] colors(Settings settings) {
        String[] colors = new String[com.github.blade.hvac.api.Wind.Level.values().length];
        for (com.github.blade.hvac.api.Wind.Level level : com.github.blade.hvac.api.Wind.Level.values())
            colors[level.ordinal()] = String.format(Locale.US, "#%06X", settings.color(level).getRGB() & 0xFFFFFF);
        return colors;
    }

    /** The script itself, from MapPeek's jar. */
    static byte[] script() throws IOException {
        try (InputStream script = WindFlow.class.getResourceAsStream("/web/wind.js")) {
            if (script == null) throw new IOException("wind.js is missing from MapPeek's jar");
            return script.readAllBytes();
        }
    }

    /** The world's name in squaremap's folders: {@code minecraft_overworld}. */
    static String folderName(World world) {
        try {
            return world.getKey().getNamespace() + "_" + world.getKey().getKey();
        } catch (RuntimeException | LinkageError noKey) {
            return world.getName();
        }
    }

    /** Installs the script and its line in squaremap's page. IO: off the main thread. */
    static void install(Path web, Consumer<String> warn) {
        try {
            Path folder = web.resolve(FOLDER);
            Files.createDirectories(folder);
            byte[] script = script();
            Path target = web.resolve(SCRIPT);
            // Only when it changed: squaremap's page needn't see a new file every refresh.
            if (!Files.isRegularFile(target) || !java.util.Arrays.equals(Files.readAllBytes(target), script))
                Files.write(target, script);
            Path index = web.resolve("index.html");
            if (!Files.isRegularFile(index)) return;
            String page = Files.readString(index, StandardCharsets.UTF_8);
            String withScript = inject(page);
            if (!withScript.equals(page)) Files.writeString(index, withScript, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            warn.accept("Couldn't add the wind animation to squaremap's page: " + failure.getMessage());
        }
    }

    /** Writes a world's wind for the script. IO: off the main thread. */
    static void publish(Path web, String world, String field, Consumer<String> warn) {
        try {
            Path folder = web.resolve(FOLDER);
            Files.createDirectories(folder);
            Path target = folder.resolve("wind-" + world + ".json");
            Path partial = folder.resolve("wind-" + world + ".json.part");
            Files.writeString(partial, field, StandardCharsets.UTF_8);
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException failure) {
            warn.accept("Couldn't write the wind for squaremap's animation: " + failure.getMessage());
        }
    }

    /** Takes the script, its line and the wind files away again. IO: off the main thread. */
    static void uninstall(Path web) {
        try {
            Path index = web.resolve("index.html");
            if (Files.isRegularFile(index)) {
                String page = Files.readString(index, StandardCharsets.UTF_8);
                String without = remove(page);
                if (!without.equals(page)) Files.writeString(index, without, StandardCharsets.UTF_8);
            }
            Path folder = web.resolve(FOLDER);
            if (Files.isDirectory(folder))
                try (var files = Files.list(folder)) {
                    for (Path file : files.toList()) Files.deleteIfExists(file);
                }
            Files.deleteIfExists(folder);
        } catch (IOException ignored) {
            // Left for next time: nothing shows without the line in the page.
        }
    }
}

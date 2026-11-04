package com.github.blade.mappeek;

import org.bukkit.World;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where a layer's grid goes when it covers the whole explored map (0.2.0):
 * every 512-block region of the world saved so far, which is what a web map
 * has tiles for. The regions are read from the world's region files.
 */
final class Coverage {
    private Coverage() {}

    static final int REGION = 512;
    private static final Pattern FILE = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

    /** A grid over the explored regions: its points and the spacing they ended up at. */
    record Points(int spacing, List<double[]> points) {}

    /**
     * The world's explored regions, as {@link #key keys}; empty when its
     * region files can't be found. Minecraft 26.x keeps them under
     * {@code dimensions/<namespace>/<dimension>/region}, 1.21 under
     * {@code region}, {@code DIM-1/region} or {@code DIM1/region}.
     */
    static Set<Long> regions(World world) {
        File folder = world.getWorldFolder();
        List<File> candidates = new ArrayList<>();
        try {
            var key = world.getKey();
            candidates.add(new File(folder, "dimensions/" + key.getNamespace() + "/" + key.getKey() + "/region"));
        } catch (RuntimeException | LinkageError noKey) {
            // Only a world's folder to go on.
        }
        candidates.add(new File(folder, "region"));
        candidates.add(new File(folder, "DIM-1/region"));
        candidates.add(new File(folder, "DIM1/region"));
        for (File directory : candidates) {
            String[] names = directory.list();
            if (names == null) continue;
            Set<Long> regions = new HashSet<>();
            for (String name : names) {
                Matcher match = FILE.matcher(name);
                if (match.matches()) regions.add(key(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2))));
            }
            if (!regions.isEmpty()) return regions;
        }
        return Set.of();
    }

    static long key(int regionX, int regionZ) {
        return ((long) regionX << 32) ^ (regionZ & 0xFFFF_FFFFL);
    }

    static int regionX(long key) { return (int) (key >> 32); }

    static int regionZ(long key) { return (int) key; }

    /**
     * Points {@code spacing} apart, on whole multiples of it, over the
     * regions; spread further apart (a whole multiple of the spacing) where
     * there would be more than {@code most}.
     */
    static Points points(Set<Long> regions, int spacing, int most) {
        int step = spacing;
        double count = Math.pow((double) REGION / spacing, 2) * regions.size();
        if (count > most) step = (int) (spacing * Math.ceil(Math.sqrt(count / most)));
        List<double[]> points = new ArrayList<>();
        for (long region : regions) {
            int fromX = regionX(region) * REGION, fromZ = regionZ(region) * REGION;
            // The multiples of the step inside this region: each belongs to exactly one region.
            long firstX = Math.floorDiv(fromX + step - 1, step) * (long) step;
            long firstZ = Math.floorDiv(fromZ + step - 1, step) * (long) step;
            for (long x = firstX; x < fromX + REGION; x += step)
                for (long z = firstZ; z < fromZ + REGION; z += step)
                    points.add(new double[]{x, z});
        }
        return new Points(step, points);
    }
}

package com.github.blade.mappeek;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A weather station's icon on the map (0.2.0), drawn here so MapPeek ships no
 * image files: a disc in the station's colour with a white mast, its three
 * lightning-rod arms and a wind vane on top, as the tower in game looks.
 */
final class StationIcon {
    private StationIcon() {}

    static final int SIZE = 32;

    static BufferedImage image(Color color) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 90));
            g.fillOval(2, 3, 28, 28);
            g.setColor(color);
            g.fillOval(1, 1, 28, 28);
            g.setColor(color.darker());
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(1, 1, 28, 28);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(15, 9, 15, 24);        // the mast
            g.drawLine(10, 24, 20, 24);       // its base
            g.drawLine(15, 13, 19, 13);       // rods: right, left, right
            g.drawLine(11, 17, 15, 17);
            g.drawLine(15, 21, 19, 21);
            g.fillPolygon(new Polygon(new int[]{15, 23, 15}, new int[]{5, 7, 9}, 3));   // the vane
        } finally {
            g.dispose();
        }
        return image;
    }

    /** The icon as PNG bytes, for maps that take files (BlueMap). */
    static byte[] png(Color color) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image(color), "png", out);
            return out.toByteArray();
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
    }
}

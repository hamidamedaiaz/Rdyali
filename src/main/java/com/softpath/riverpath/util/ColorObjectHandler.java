package com.softpath.riverpath.util;

import javafx.scene.paint.Color;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Class to handle color object
 *
 * @author rhajou
 */
@Data
@NoArgsConstructor
public class ColorObjectHandler {

    private static final List<Color> IMMERSED_OBJECT_COLORS = Arrays.asList(Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE, Color.CYAN, Color.AQUAMARINE, Color.SIENNA, Color.MAGENTA, Color.RED,Color.YELLOWGREEN);
    private int colorIndex = 0;

    public Color getNextColor() {
        return IMMERSED_OBJECT_COLORS.get(colorIndex++);
    }
}

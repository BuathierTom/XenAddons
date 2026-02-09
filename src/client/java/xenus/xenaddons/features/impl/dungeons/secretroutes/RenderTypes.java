package xenus.xenaddons.features.impl.dungeons.secretroutes;

import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

public class RenderTypes {
    public static class WorldText {
        public Text text;
        public Vec3d position;
        public boolean throughWalls;
        public float scale;

        public WorldText(Text text, Vec3d position, boolean throughWalls) {
            this.text = text;
            this.position = position;
            this.throughWalls = throughWalls;
            this.scale = 3.0f;
        }

        public WorldText(Text text, Vec3d position, boolean throughWalls, float scale) {
            this.text = text;
            this.position = position;
            this.throughWalls = throughWalls;
            this.scale = scale;
        }
    }

    public static class OutlinedBox {
        public Vec3d position;
        public Color color;
        public float boxWidth;
        public float boxHeight;
        public boolean throughWalls;

        public OutlinedBox(Vec3d position, Color color, float boxWidth, float boxHeight, boolean throughWalls) {
            this.position = position;
            this.color = color;
            this.boxWidth = boxWidth;
            this.boxHeight = boxHeight;
            this.throughWalls = throughWalls;
        }
    }

    public static class FilledBox {
        public Vec3d position;
        public Color color;
        public float boxWidth;
        public float boxHeight;
        public boolean throughWalls;

        public FilledBox(Vec3d position, Color color, float boxWidth, float boxHeight, boolean throughWalls) {
            this.position = position;
            this.color = color;
            this.boxWidth = boxWidth;
            this.boxHeight = boxHeight;
            this.throughWalls = throughWalls;
        }
    }

    public static class Line {
        public Vec3d start;
        public Vec3d end;
        public Color color;
        public float lineWidth;
        public boolean throughWalls;

        public Line(Vec3d start, Vec3d end, Color color, float lineWidth, boolean throughWalls) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.lineWidth = lineWidth;
            this.throughWalls = throughWalls;
        }
    }

    public static class LineFromCursor {
        public Vec3d point;
        public Color color;
        public float lineWidth;
        public boolean throughWalls;

        public LineFromCursor(Vec3d point, Color color, float lineWidth) {
            this(point, color, lineWidth, true);
        }

        public LineFromCursor(Vec3d point, Color color, float lineWidth, boolean throughWalls) {
            this.point = point;
            this.color = color;
            this.lineWidth = lineWidth;
            this.throughWalls = throughWalls;
        }
    }
}

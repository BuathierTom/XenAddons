package xenus.xenaddons.features.impl.dungeons.secretroutes;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

public class RenderTypes {
    public static class WorldText {
        public Component text;
        public Vec3 position;
        public boolean throughWalls;
        public float scale;

        public WorldText(Component text, Vec3 position, boolean throughWalls) {
            this.text = text;
            this.position = position;
            this.throughWalls = throughWalls;
            this.scale = 3.0f;
        }

        public WorldText(Component text, Vec3 position, boolean throughWalls, float scale) {
            this.text = text;
            this.position = position;
            this.throughWalls = throughWalls;
            this.scale = scale;
        }
    }

    public static class OutlinedBox {
        public Vec3 position;
        public Color color;
        public float boxWidth;
        public float boxHeight;
        public boolean throughWalls;

        public OutlinedBox(Vec3 position, Color color, float boxWidth, float boxHeight, boolean throughWalls) {
            this.position = position;
            this.color = color;
            this.boxWidth = boxWidth;
            this.boxHeight = boxHeight;
            this.throughWalls = throughWalls;
        }
    }

    public static class FilledBox {
        public Vec3 position;
        public Color color;
        public float boxWidth;
        public float boxHeight;
        public boolean throughWalls;

        public FilledBox(Vec3 position, Color color, float boxWidth, float boxHeight, boolean throughWalls) {
            this.position = position;
            this.color = color;
            this.boxWidth = boxWidth;
            this.boxHeight = boxHeight;
            this.throughWalls = throughWalls;
        }
    }

    public static class Line {
        public Vec3 start;
        public Vec3 end;
        public Color color;
        public float lineWidth;
        public boolean throughWalls;

        public Line(Vec3 start, Vec3 end, Color color, float lineWidth, boolean throughWalls) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.lineWidth = lineWidth;
            this.throughWalls = throughWalls;
        }
    }

    public static class LineFromCursor {
        public Vec3 point;
        public Color color;
        public float lineWidth;
        public boolean throughWalls;

        public LineFromCursor(Vec3 point, Color color, float lineWidth) {
            this(point, color, lineWidth, true);
        }

        public LineFromCursor(Vec3 point, Color color, float lineWidth, boolean throughWalls) {
            this.point = point;
            this.color = color;
            this.lineWidth = lineWidth;
            this.throughWalls = throughWalls;
        }
    }
}

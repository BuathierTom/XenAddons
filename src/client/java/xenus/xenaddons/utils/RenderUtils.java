package xenus.xenaddons.utils;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import xenus.xenaddons.compat.XenWorldRenderCompat;

import java.awt.Color;

public class RenderUtils {

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color); // Top
        context.fill(x, y + height - 1, x + width, y + height, color); // Bottom
        context.fill(x, y, x + 1, y + height, color); // Left
        context.fill(x + width - 1, y, x + width, y + height, color); // Right
    }

    public static void drawText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
        } else {
            context.drawText(textRenderer, text, x, y, color, false);
        }
    }
    
    public static void drawCenteredText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        int textWidth = textRenderer.getWidth(text);
        drawText(context, textRenderer, text, x - textWidth / 2, y, color, shadow);
    }

    public static int darken(int color) {
        float[] hsv = new float[3];
        Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
        hsv[2] *= 0.8f; // Reduce brightness
        return (color & 0xFF000000) | (Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 0x00FFFFFF);
    }

    public static void drawLine(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d cameraPos, Vec3d start, Vec3d end, int color, float width) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        VertexConsumer buffer = consumers.getBuffer(XenWorldRenderCompat.normalLineLayer());
        
        matrices.push();
        matrices.translate(start.x - cameraPos.x, start.y - cameraPos.y, start.z - cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float lineWidth = Math.max(1.0f, width);
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a).normal(0, 1, 0).lineWidth(lineWidth);
        buffer.vertex(matrix, (float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z)).color(r, g, b, a).normal(0, 1, 0).lineWidth(lineWidth);

        matrices.pop();
    }

    public static void drawBox(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d cameraPos, Box box, int color, boolean filled) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a == 0) a = 0.5f;

        VertexConsumer buffer = consumers.getBuffer(filled ? XenWorldRenderCompat.normalQuadLayer() : XenWorldRenderCompat.normalLineLayer());
        
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (filled) {
            drawBoxFaces(matrix, buffer, box, r, g, b, a);
        } else {
            drawBoxOutline(matrix, buffer, box, r, g, b, a);
        }

        matrices.pop();
    }

    private static void drawBoxFaces(Matrix4f matrix, VertexConsumer buffer, Box box, float r, float g, float b, float a) {
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;

        // Bottom
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, -1, 0);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, -1, 0);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, -1, 0);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, -1, 0);
        
        // Top
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
        
        // North
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 0, -1);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 0, -1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 0, -1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 0, -1);
        
        // South
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 0, 1);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 0, 1);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 0, 1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 0, 1);
        
        // West
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(-1, 0, 0);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(-1, 0, 0);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(-1, 0, 0);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(-1, 0, 0);
        
        // East
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(1, 0, 0);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(1, 0, 0);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(1, 0, 0);
    }

    private static void drawBoxOutline(Matrix4f matrix, VertexConsumer buffer, Box box, float r, float g, float b, float a) {
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;

        // Bottom
        lineVertex(matrix, buffer, minX, minY, minZ, r, g, b, a, 0, -1, 0);
        lineVertex(matrix, buffer, maxX, minY, minZ, r, g, b, a, 0, -1, 0);
        lineVertex(matrix, buffer, maxX, minY, minZ, r, g, b, a, 0, -1, 0);
        lineVertex(matrix, buffer, maxX, minY, maxZ, r, g, b, a, 0, -1, 0);
        lineVertex(matrix, buffer, maxX, minY, maxZ, r, g, b, a, 0, -1, 0);
        lineVertex(matrix, buffer, minX, minY, maxZ, r, g, b, a, 0, -1, 0);
        lineVertex(matrix, buffer, minX, minY, maxZ, r, g, b, a, 0, -1, 0);
        lineVertex(matrix, buffer, minX, minY, minZ, r, g, b, a, 0, -1, 0);
        
        // Top
        lineVertex(matrix, buffer, minX, maxY, minZ, r, g, b, a, 0, 1, 0);
        lineVertex(matrix, buffer, maxX, maxY, minZ, r, g, b, a, 0, 1, 0);
        lineVertex(matrix, buffer, maxX, maxY, minZ, r, g, b, a, 0, 1, 0);
        lineVertex(matrix, buffer, maxX, maxY, maxZ, r, g, b, a, 0, 1, 0);
        lineVertex(matrix, buffer, maxX, maxY, maxZ, r, g, b, a, 0, 1, 0);
        lineVertex(matrix, buffer, minX, maxY, maxZ, r, g, b, a, 0, 1, 0);
        lineVertex(matrix, buffer, minX, maxY, maxZ, r, g, b, a, 0, 1, 0);
        lineVertex(matrix, buffer, minX, maxY, minZ, r, g, b, a, 0, 1, 0);
        
        // Verticals
        lineVertex(matrix, buffer, minX, minY, minZ, r, g, b, a, -1, 0, -1);
        lineVertex(matrix, buffer, minX, maxY, minZ, r, g, b, a, -1, 0, -1);
        lineVertex(matrix, buffer, maxX, minY, minZ, r, g, b, a, 1, 0, -1);
        lineVertex(matrix, buffer, maxX, maxY, minZ, r, g, b, a, 1, 0, -1);
        lineVertex(matrix, buffer, maxX, minY, maxZ, r, g, b, a, 1, 0, 1);
        lineVertex(matrix, buffer, maxX, maxY, maxZ, r, g, b, a, 1, 0, 1);
        lineVertex(matrix, buffer, minX, minY, maxZ, r, g, b, a, -1, 0, 1);
        lineVertex(matrix, buffer, minX, maxY, maxZ, r, g, b, a, -1, 0, 1);
    }

    private static void lineVertex(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, float r, float g, float b, float a, float nx, float ny, float nz) {
        buffer.vertex(matrix, x, y, z).color(r, g, b, a).normal(nx, ny, nz).lineWidth(1.0f);
    }
}

package xenus.xenaddons.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import xenus.xenaddons.compat.XenWorldRenderCompat;

import java.awt.Color;

public class RenderUtils {

    public static void drawBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color); // Top
        context.fill(x, y + height - 1, x + width, y + height, color); // Bottom
        context.fill(x, y, x + 1, y + height, color); // Left
        context.fill(x + width - 1, y, x + width, y + height, color); // Right
    }

    public static void drawText(GuiGraphicsExtractor context, Font textRenderer, String text, int x, int y, int color, boolean shadow) {
        context.text(textRenderer, text, x, y, color, shadow);
    }
    
    public static void drawCenteredText(GuiGraphicsExtractor context, Font textRenderer, String text, int x, int y, int color, boolean shadow) {
        int textWidth = textRenderer.width(text);
        drawText(context, textRenderer, text, x - textWidth / 2, y, color, shadow);
    }

    public static int darken(int color) {
        float[] hsv = new float[3];
        Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
        hsv[2] *= 0.8f; // Reduce brightness
        return (color & 0xFF000000) | (Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 0x00FFFFFF);
    }

    public static void drawLine(PoseStack matrices, MultiBufferSource consumers, Vec3 cameraPos, Vec3 start, Vec3 end, int color, float width) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        VertexConsumer buffer = consumers.getBuffer(XenWorldRenderCompat.normalLineLayer());
        
        matrices.pushPose();
        matrices.translate(start.x - cameraPos.x, start.y - cameraPos.y, start.z - cameraPos.z);
        Matrix4f matrix = matrices.last().pose();

        float lineWidth = Math.max(1.0f, width);
        buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(lineWidth);
        buffer.addVertex(matrix, (float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z)).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(lineWidth);

        matrices.popPose();
    }

    public static void drawBox(PoseStack matrices, MultiBufferSource consumers, Vec3 cameraPos, AABB box, int color, boolean filled) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a == 0) a = 0.5f;

        VertexConsumer buffer = consumers.getBuffer(filled ? XenWorldRenderCompat.normalQuadLayer() : XenWorldRenderCompat.normalLineLayer());
        
        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrices.last().pose();

        if (filled) {
            drawBoxFaces(matrix, buffer, box, r, g, b, a);
        } else {
            drawBoxOutline(matrix, buffer, box, r, g, b, a);
        }

        matrices.popPose();
    }

    private static void drawBoxFaces(Matrix4f matrix, VertexConsumer buffer, AABB box, float r, float g, float b, float a) {
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;

        // Bottom
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setNormal(0, -1, 0);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setNormal(0, -1, 0);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(0, -1, 0);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setNormal(0, -1, 0);
        
        // Top
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setNormal(0, 1, 0);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(0, 1, 0);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(0, 1, 0);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(0, 1, 0);
        
        // North
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setNormal(0, 0, -1);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setNormal(0, 0, -1);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(0, 0, -1);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setNormal(0, 0, -1);
        
        // South
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setNormal(0, 0, 1);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(0, 0, 1);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(0, 0, 1);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(0, 0, 1);
        
        // West
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setNormal(-1, 0, 0);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setNormal(-1, 0, 0);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(-1, 0, 0);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setNormal(-1, 0, 0);
        
        // East
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setNormal(1, 0, 0);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(1, 0, 0);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(1, 0, 0);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(1, 0, 0);
    }

    private static void drawBoxOutline(Matrix4f matrix, VertexConsumer buffer, AABB box, float r, float g, float b, float a) {
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
        buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(1.0f);
    }

    public static void drawCircleOutline(PoseStack matrices, MultiBufferSource consumers, Vec3 cameraPos, Vec3 center, float radius, int color, int segments) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        VertexConsumer buffer = consumers.getBuffer(XenWorldRenderCompat.normalLineLayer());

        matrices.pushPose();
        matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
        Matrix4f matrix = matrices.last().pose();

        for (int i = 0; i < segments; i++) {
            double angle1 = (2 * Math.PI * i) / segments;
            double angle2 = (2 * Math.PI * (i + 1)) / segments;
            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);

            buffer.addVertex(matrix, x1, 0, z1).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(2.0f);
            buffer.addVertex(matrix, x2, 0, z2).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(2.0f);
        }

        matrices.popPose();
    }

    public static void drawCircleFilled(PoseStack matrices, MultiBufferSource consumers, Vec3 cameraPos, Vec3 center, float radius, int color, int segments) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a == 0) a = 0.5f;

        VertexConsumer buffer = consumers.getBuffer(XenWorldRenderCompat.normalQuadLayer());

        matrices.pushPose();
        matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
        Matrix4f matrix = matrices.last().pose();

        for (int i = 0; i < segments; i++) {
            double angle1 = (2 * Math.PI * i) / segments;
            double angle2 = (2 * Math.PI * (i + 1)) / segments;
            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);

            // Degenerate quad (last two vertices repeated) draws as a triangle wedge from the center.
            buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x1, 0, z1).setColor(r, g, b, a).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x2, 0, z2).setColor(r, g, b, a).setNormal(0, 1, 0);
            buffer.addVertex(matrix, x2, 0, z2).setColor(r, g, b, a).setNormal(0, 1, 0);
        }

        matrices.popPose();
    }
}

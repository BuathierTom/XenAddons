package xenus.xenaddons.features.impl.dungeons.secretroutes;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class AnotherRenderingUtil {
    private static final float THICKNESS_MULTIPLIER = 0.01f;

    private static final RenderPipeline SEE_THROUGH_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("secret_routes/see_through"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .build()
    );
    private static final RenderType SEE_THROUGH_LAYER = RenderType.create(
        "xenaddons_secret_routes_see_through",
        RenderSetup.builder(SEE_THROUGH_PIPELINE).createRenderSetup()
    );

    private static final RenderPipeline NORMAL_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("secret_routes/normal"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withCull(false)
            .build()
    );
    private static final RenderType NORMAL_LAYER = RenderType.create(
        "xenaddons_secret_routes_normal",
        RenderSetup.builder(NORMAL_PIPELINE).createRenderSetup()
    );

    private static final RenderPipeline CURSOR_LINE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(id("secret_routes/cursor_line"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
            .withDepthStencilState(Optional.empty())
            .build()
    );
    private static final RenderType CURSOR_LINE_LAYER = RenderType.create(
        "xenaddons_secret_routes_cursor_line",
        RenderSetup.builder(CURSOR_LINE_PIPELINE).createRenderSetup()
    );

    private static final List<RenderTypes.WorldText> worldTexts = new ArrayList<>();
    private static final List<RenderTypes.OutlinedBox> outlinedBoxes = new ArrayList<>();
    private static final List<RenderTypes.FilledBox> filledBoxes = new ArrayList<>();
    private static final List<RenderTypes.Line> lines = new ArrayList<>();
    private static final List<RenderTypes.LineFromCursor> linesFromCursor = new ArrayList<>();

    private static final Vector3f scratchLineDirection = new Vector3f();

    public static float filledBoxAlpha = 0.5f;
    public static float boxLineWidth = 2.0f;
    public static Runnable preRenderCallback;

    private AnotherRenderingUtil() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(AnotherRenderingUtil::collect);
        LevelRenderEvents.END_MAIN.register(AnotherRenderingUtil::cleanup);
    }

    private static void collect(LevelRenderContext context) {
        if (preRenderCallback != null) {
            preRenderCallback.run();
        }

        PoseStack poseStack = context.poseStack();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.position();
        OrderedSubmitNodeCollector collector = context.submitNodeCollector();

        boolean hasSeeThroughGeometry = hasGeometry(true);
        boolean hasNormalGeometry = hasGeometry(false);

        if (hasSeeThroughGeometry) {
            collector.submitCustomGeometry(poseStack, SEE_THROUGH_LAYER, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                renderFilledBoxes(buffer, matrix, cameraPosition, true);
                renderOutlinedBoxes(buffer, matrix, cameraPosition, true);
                renderLines(buffer, matrix, cameraPosition, true);
            });
        }

        if (hasNormalGeometry) {
            collector.submitCustomGeometry(poseStack, NORMAL_LAYER, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                renderFilledBoxes(buffer, matrix, cameraPosition, false);
                renderOutlinedBoxes(buffer, matrix, cameraPosition, false);
                renderLines(buffer, matrix, cameraPosition, false);
            });
        }

        if (!linesFromCursor.isEmpty()) {
            collector.submitCustomGeometry(
                poseStack,
                CURSOR_LINE_LAYER,
                (pose, buffer) -> renderLinesFromCursor(buffer, pose, cameraPosition)
            );
        }

        submitWorldTexts(collector, poseStack, camera, cameraPosition);
    }

    private static boolean hasGeometry(boolean throughWalls) {
        for (RenderTypes.FilledBox box : filledBoxes) {
            if (box.throughWalls == throughWalls) return true;
        }
        for (RenderTypes.OutlinedBox box : outlinedBoxes) {
            if (box.throughWalls == throughWalls) return true;
        }
        for (RenderTypes.Line line : lines) {
            if (line.throughWalls == throughWalls) return true;
        }
        return false;
    }

    private static void submitWorldTexts(
        OrderedSubmitNodeCollector collector,
        PoseStack poseStack,
        Camera camera,
        Vec3 cameraPosition
    ) {
        if (worldTexts.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        Quaternionf cameraRotation = camera.rotation();

        for (RenderTypes.WorldText worldText : worldTexts) {
            poseStack.pushPose();
            poseStack.translate(
                worldText.position.x - cameraPosition.x,
                worldText.position.y - cameraPosition.y,
                worldText.position.z - cameraPosition.z
            );
            poseStack.mulPose(cameraRotation);

            float scale = 0.025f * worldText.scale;
            poseStack.scale(scale, -scale, scale);

            Font.DisplayMode displayMode = worldText.throughWalls
                ? Font.DisplayMode.SEE_THROUGH
                : Font.DisplayMode.NORMAL;
            float xOffset = -font.width(worldText.text) / 2.0f;

            collector.submitText(
                poseStack,
                xOffset,
                0,
                worldText.text.getVisualOrderText(),
                true,
                displayMode,
                0xF000F0,
                0xFFFFFFFF,
                0,
                0
            );
            poseStack.popPose();
        }
    }

    private static void renderFilledBoxes(
        VertexConsumer buffer,
        Matrix4f matrix,
        Vec3 cameraPosition,
        boolean throughWalls
    ) {
        float cameraX = (float) cameraPosition.x;
        float cameraY = (float) cameraPosition.y;
        float cameraZ = (float) cameraPosition.z;
        float configuredAlpha = Math.max(0.0f, Math.min(1.0f, filledBoxAlpha));

        for (RenderTypes.FilledBox box : filledBoxes) {
            if (box.throughWalls != throughWalls) continue;

            float minX = (float) box.position.x - cameraX;
            float minY = (float) box.position.y - cameraY;
            float minZ = (float) box.position.z - cameraZ;
            float maxX = minX + box.boxWidth;
            float maxY = minY + box.boxHeight;
            float maxZ = minZ + box.boxWidth;

            drawBoxFaces(
                buffer,
                matrix,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                box.color.getRed() / 255.0f,
                box.color.getGreen() / 255.0f,
                box.color.getBlue() / 255.0f,
                configuredAlpha * (box.color.getAlpha() / 255.0f)
            );
        }
    }

    private static void renderOutlinedBoxes(
        VertexConsumer buffer,
        Matrix4f matrix,
        Vec3 cameraPosition,
        boolean throughWalls
    ) {
        float thickness = Math.max(0.002f, boxLineWidth * THICKNESS_MULTIPLIER);

        for (RenderTypes.OutlinedBox box : outlinedBoxes) {
            if (box.throughWalls != throughWalls) continue;

            Vec3 min = box.position.subtract(cameraPosition);
            Vec3 max = min.add(box.boxWidth, box.boxHeight, box.boxWidth);
            float red = box.color.getRed() / 255.0f;
            float green = box.color.getGreen() / 255.0f;
            float blue = box.color.getBlue() / 255.0f;
            float alpha = box.color.getAlpha() / 255.0f;

            drawBoxEdges(buffer, matrix, min, max, red, green, blue, alpha, thickness);
        }
    }

    private static void renderLines(
        VertexConsumer buffer,
        Matrix4f matrix,
        Vec3 cameraPosition,
        boolean throughWalls
    ) {
        for (RenderTypes.Line line : lines) {
            if (line.throughWalls != throughWalls) continue;

            Vec3 start = line.start.subtract(cameraPosition);
            Vec3 end = line.end.subtract(cameraPosition);
            drawBillboardLine(
                buffer,
                matrix,
                start,
                end,
                line.color.getRed() / 255.0f,
                line.color.getGreen() / 255.0f,
                line.color.getBlue() / 255.0f,
                line.color.getAlpha() / 255.0f,
                Math.max(0.002f, line.lineWidth * THICKNESS_MULTIPLIER)
            );
        }
    }

    private static void renderLinesFromCursor(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        Vec3 cameraPosition
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 start = minecraft.player.getEyePosition(partialTick)
            .add(minecraft.player.getLookAngle().scale(2.0));
        float startX = (float) (start.x - cameraPosition.x);
        float startY = (float) (start.y - cameraPosition.y);
        float startZ = (float) (start.z - cameraPosition.z);
        Matrix4f matrix = pose.pose();

        for (RenderTypes.LineFromCursor line : linesFromCursor) {
            float endX = (float) (line.point.x - cameraPosition.x);
            float endY = (float) (line.point.y - cameraPosition.y);
            float endZ = (float) (line.point.z - cameraPosition.z);

            scratchLineDirection.set(endX - startX, endY - startY, endZ - startZ);
            if (scratchLineDirection.lengthSquared() < 0.0001f) continue;
            scratchLineDirection.normalize();

            float red = line.color.getRed() / 255.0f;
            float green = line.color.getGreen() / 255.0f;
            float blue = line.color.getBlue() / 255.0f;
            float alpha = line.color.getAlpha() / 255.0f;
            float width = Math.max(1.0f, line.lineWidth);

            buffer.addVertex(matrix, startX, startY, startZ)
                .setNormal(pose, scratchLineDirection.x, scratchLineDirection.y, scratchLineDirection.z)
                .setColor(red, green, blue, alpha)
                .setLineWidth(width);
            buffer.addVertex(matrix, endX, endY, endZ)
                .setNormal(pose, scratchLineDirection.x, scratchLineDirection.y, scratchLineDirection.z)
                .setColor(red, green, blue, alpha)
                .setLineWidth(width);
        }
    }

    private static void drawBoxEdges(
        VertexConsumer buffer,
        Matrix4f matrix,
        Vec3 min,
        Vec3 max,
        float red,
        float green,
        float blue,
        float alpha,
        float thickness
    ) {
        Vec3 c000 = new Vec3(min.x, min.y, min.z);
        Vec3 c100 = new Vec3(max.x, min.y, min.z);
        Vec3 c010 = new Vec3(min.x, max.y, min.z);
        Vec3 c110 = new Vec3(max.x, max.y, min.z);
        Vec3 c001 = new Vec3(min.x, min.y, max.z);
        Vec3 c101 = new Vec3(max.x, min.y, max.z);
        Vec3 c011 = new Vec3(min.x, max.y, max.z);
        Vec3 c111 = new Vec3(max.x, max.y, max.z);

        drawBillboardLine(buffer, matrix, c000, c100, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c100, c101, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c101, c001, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c001, c000, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c010, c110, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c110, c111, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c111, c011, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c011, c010, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c000, c010, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c100, c110, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c001, c011, red, green, blue, alpha, thickness);
        drawBillboardLine(buffer, matrix, c101, c111, red, green, blue, alpha, thickness);
    }

    private static void drawBillboardLine(
        VertexConsumer buffer,
        Matrix4f matrix,
        Vec3 start,
        Vec3 end,
        float red,
        float green,
        float blue,
        float alpha,
        float thickness
    ) {
        Vector3f direction = new Vector3f(
            (float) (end.x - start.x),
            (float) (end.y - start.y),
            (float) (end.z - start.z)
        );
        if (direction.lengthSquared() < 0.000001f) return;
        direction.normalize();

        Vector3f cameraDirection = new Vector3f(
            (float) ((start.x + end.x) * 0.5),
            (float) ((start.y + end.y) * 0.5),
            (float) ((start.z + end.z) * 0.5)
        );
        if (cameraDirection.lengthSquared() < 0.0001f) {
            cameraDirection.set(0, 1, 0);
        } else {
            cameraDirection.normalize();
        }

        Vector3f widthDirection = new Vector3f();
        direction.cross(cameraDirection, widthDirection);
        if (widthDirection.lengthSquared() < 0.0001f) {
            widthDirection.set(Math.abs(direction.x) > 0.9f ? 0 : 1, Math.abs(direction.x) > 0.9f ? 1 : 0, 0);
        }
        widthDirection.normalize().mul(thickness / 2.0f);

        float widthX = widthDirection.x;
        float widthY = widthDirection.y;
        float widthZ = widthDirection.z;
        int light = 0xF000F0;

        buffer.addVertex(matrix, (float) start.x - widthX, (float) start.y - widthY, (float) start.z - widthZ)
            .setColor(red, green, blue, alpha).setLight(light);
        buffer.addVertex(matrix, (float) start.x + widthX, (float) start.y + widthY, (float) start.z + widthZ)
            .setColor(red, green, blue, alpha).setLight(light);
        buffer.addVertex(matrix, (float) end.x + widthX, (float) end.y + widthY, (float) end.z + widthZ)
            .setColor(red, green, blue, alpha).setLight(light);
        buffer.addVertex(matrix, (float) end.x - widthX, (float) end.y - widthY, (float) end.z - widthZ)
            .setColor(red, green, blue, alpha).setLight(light);
    }

    private static void drawBoxFaces(
        VertexConsumer buffer,
        Matrix4f matrix,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        int light = 0xF000F0;

        addVertex(buffer, matrix, x1, y1, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x1, y1, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y1, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y1, z2, red, green, blue, alpha, light);

        addVertex(buffer, matrix, x1, y2, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x1, y2, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y2, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y2, z1, red, green, blue, alpha, light);

        addVertex(buffer, matrix, x1, y1, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x1, y2, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y2, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y1, z1, red, green, blue, alpha, light);

        addVertex(buffer, matrix, x2, y1, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y2, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x1, y2, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x1, y1, z2, red, green, blue, alpha, light);

        addVertex(buffer, matrix, x1, y1, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x1, y2, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x1, y2, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x1, y1, z1, red, green, blue, alpha, light);

        addVertex(buffer, matrix, x2, y1, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y2, z1, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y2, z2, red, green, blue, alpha, light);
        addVertex(buffer, matrix, x2, y1, z2, red, green, blue, alpha, light);
    }

    private static void addVertex(
        VertexConsumer buffer,
        Matrix4f matrix,
        float x,
        float y,
        float z,
        float red,
        float green,
        float blue,
        float alpha,
        int light
    ) {
        buffer.addVertex(matrix, x, y, z)
            .setColor(red, green, blue, alpha)
            .setLight(light);
    }

    private static void cleanup(LevelRenderContext context) {
        outlinedBoxes.clear();
        filledBoxes.clear();
        lines.clear();
        linesFromCursor.clear();
        worldTexts.clear();
    }

    public static void addWorldText(RenderTypes.WorldText worldText) {
        worldTexts.add(worldText);
    }

    public static void addOutlinedBox(RenderTypes.OutlinedBox outlinedBox) {
        outlinedBoxes.add(outlinedBox);
    }

    public static void addFilledBox(RenderTypes.FilledBox filledBox) {
        filledBoxes.add(filledBox);
    }

    public static void addLine(RenderTypes.Line line) {
        lines.add(line);
    }

    public static void addLinesFromPoints(Vec3[] points, Color color, float lineWidth, boolean throughWalls) {
        for (int index = 0; index < points.length - 1; index++) {
            addLine(new RenderTypes.Line(points[index], points[index + 1], color, lineWidth, throughWalls));
        }
    }

    public static void addLineFromCursor(RenderTypes.LineFromCursor lineFromCursor) {
        linesFromCursor.add(lineFromCursor);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("xenaddons", path);
    }
}

package xenus.xenaddons.compat;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

public final class XenCameraCompat {
    private XenCameraCompat() {}

    public static Vec3d getPos(Camera camera) {
        return camera.getPos();
    }
}

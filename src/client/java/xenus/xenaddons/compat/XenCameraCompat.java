package xenus.xenaddons.compat;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

public final class XenCameraCompat {
    private XenCameraCompat() {}

    public static Vec3 getPos(Camera camera) {
        return camera.position();
    }
}

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.2"

stonecutter parameters {
    val gui = current.parsed >= "26.2"
    val sdl = current.parsed >= "26.3"

    replacements.string(gui) {
        replace("Minecraft.getInstance().screen", "Minecraft.getInstance().gui.screen()")
        replace("client.screen", "client.gui.screen()")
        replace("mc.screen", "mc.gui.screen()")
        replace("Minecraft.getInstance().setScreen(", "Minecraft.getInstance().gui.setScreen(")
        replace("client.setScreen(", "client.gui.setScreen(")
        replace("minecraft.setScreen(", "minecraft.gui.setScreen(")
        replace("mc.setScreen(", "mc.gui.setScreen(")
        replace("gameRenderer.getMainCamera()", "gameRenderer.mainCamera()")
        replace("new XenNvgSpecialElementRenderer(context.bufferSource())", "new XenNvgSpecialElementRenderer()")
        replace("import net.minecraft.client.gui.Gui;", "import net.minecraft.client.gui.Hud;")
        replace("@Mixin(Gui.class)", "@Mixin(Hud.class)")
        replace(".gui.getChat()", ".gui.hud.getChat()")
        replace(".gui.getTabList()", ".gui.hud.getTabList()")
        replace(".gui.setTitle(", ".gui.hud.setTitle(")
        replace(".gui.setTimes(", ".gui.hud.setTimes(")
        replace("mc.options.hideGui", "mc.gui.hud.isHidden()")
        replace("client.getOverlay()", "client.gui.overlay()")
        replace("mc.getOverlay()", "mc.gui.overlay()")
        replace("minecraft.getMainRenderTarget()", "minecraft.gameRenderer.mainRenderTarget()")
        replace("Blocks.BLUE_TERRACOTTA", "Blocks.DYED_TERRACOTTA.blue()")
    }

    listOf(
        (gui && !sdl) to "com.mojang.blaze3d.PrimitiveTopology",
        sdl to "com.mojang.renderpearl.api.pipeline.PrimitiveTopology",
    ).forEach { (enabled, topology) ->
        replacements.regex(enabled) {
            replace(
                "\\.withVertexFormat\\(([\\w.]+), VertexFormat\\.Mode\\.(\\w+)\\)",
                ".withVertexBinding(0, \$1).withPrimitiveTopology($topology.\$2)",
                "\\.withVertexBinding\\(0, ([\\w.]+)\\)\\.withPrimitiveTopology\\(${Regex.escape(topology)}\\.(\\w+)\\)",
                ".withVertexFormat(\$1, VertexFormat.Mode.\$2)",
            )
        }
    }

    replacements.string(sdl) {
        replace("com.mojang.blaze3d.opengl.", "com.mojang.renderpearl.backend.opengl.")
        replace("com.mojang.blaze3d.pipeline.RenderPipeline", "com.mojang.renderpearl.api.pipeline.RenderPipeline")
        replace("com.mojang.blaze3d.pipeline.BlendFunction", "com.mojang.renderpearl.api.pipeline.BlendFunction")
        replace("com.mojang.blaze3d.pipeline.ColorTargetState", "com.mojang.renderpearl.api.pipeline.ColorTargetState")
        replace("com.mojang.blaze3d.pipeline.DepthStencilState", "com.mojang.renderpearl.api.pipeline.DepthStencilState")
        replace("com.mojang.blaze3d.textures.", "com.mojang.renderpearl.api.textures.")
        replace("com.mojang.blaze3d.vertex.VertexFormat", "com.mojang.renderpearl.api.vertex.VertexFormat")
        replace("com.mojang.blaze3d.platform.CompareOp", "com.mojang.renderpearl.api.pipeline.CompareOp")
        replace("com.mojang.blaze3d.systems.GpuDevice", "com.mojang.renderpearl.api.device.GpuDevice")
        replace("com.mojang.blaze3d.GpuFormat", "com.mojang.renderpearl.api.GpuFormat")
        replace("com.mojang.blaze3d.PrimitiveTopology", "com.mojang.renderpearl.api.pipeline.PrimitiveTopology")
        replace("InputConstants.KEY_LSUPER", "InputConstants.KEY_LGUI")
        replace("InputConstants.KEY_RSUPER", "InputConstants.KEY_RGUI")
        replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
        replace("click.button()", "xenus.xenaddons.compat.XenInputCompat.button(click.button())")
        replace("input.button()", "xenus.xenaddons.compat.XenInputCompat.button(input.button())")
        replace("-100 + bound.getValue()", "-100 + xenus.xenaddons.compat.XenInputCompat.button(bound.getValue())")
        replace("input.scancode()", "input.keycode()")
        replace("Frustum;DDD)Z", "Frustum;DDDF)Z")
        replace("double x, double y, double z, CallbackInfoReturnable<Boolean> cir", "double x, double y, double z, float partialTick, CallbackInfoReturnable<Boolean> cir")
        replace("NativeImage.read(NativeImage.Format.RGBA, stream)", "NativeImage.read(stream)")
        replace("NativeImage.read(NativeImage.Format.RGBA, new ByteArrayInputStream(imageBytes))", "NativeImage.read(new ByteArrayInputStream(imageBytes))")
        replace("Util.getPlatform().openFile(file)", "com.mojang.blaze3d.Blaze3D.openPath(file.toPath())")
        replace("poseStack.mulPose(cameraRotation)", "poseStack.mulPose(new org.joml.Matrix4f().rotation(cameraRotation))")
        replace("packet.getParticle()", "packet.particle()")
        replace("packet.getCount()", "packet.count()")
        replace("packet.getXDist()", "packet.xDist()")
        replace("packet.getMaxSpeed()", "packet.xMaxSpeed()")
        replace("packet.getX()", "packet.x()")
        replace("packet.getZ()", "packet.z()")
    }
}

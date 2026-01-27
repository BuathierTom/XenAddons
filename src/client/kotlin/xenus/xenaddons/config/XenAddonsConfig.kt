package xenus.xenaddons.config

import java.io.File
import java.util.Properties
import net.fabricmc.loader.api.FabricLoader

object XenAddonsConfig {
    var itemScaleSize: Float = 0f
    var scaleSwing: Boolean = false
    var itemScaleX: Float = 0f
    var itemScaleY: Float = 0f
    var itemScaleZ: Float = 0f
    var itemScaleYaw: Float = 0f
    var itemScalePitch: Float = 0f
    var itemScaleRoll: Float = 0f
    var itemScaleSpeed: Float = 0f

    var ignoreHaste: Boolean = false
    var noEquipReset: Boolean = false
    var noSwing: Boolean = false
    var noTerminatorSwing: Boolean = false
    
    // Dungeons
    var autoRequeue: Boolean = false
    var secretsSound: Boolean = false
    var dungeonDeathsDisplay: Boolean = false
    
    // Solvers
    var lividSolver: Boolean = false
    var lividBoxColor: Int = 0x00FFFF
    var lividLineWidth: Float = 3f
    var lividTracer: Boolean = true
    var lividHideWrong: Boolean = false

    private val file: File
        get() = FabricLoader.getInstance().configDir.resolve("xenaddons.properties").toFile()

    fun save() {
        val props = Properties()
        props.setProperty("itemScaleSize", itemScaleSize.toString())
        props.setProperty("scaleSwing", scaleSwing.toString())
        props.setProperty("itemScaleX", itemScaleX.toString())
        props.setProperty("itemScaleY", itemScaleY.toString())
        props.setProperty("itemScaleZ", itemScaleZ.toString())
        props.setProperty("itemScaleYaw", itemScaleYaw.toString())
        props.setProperty("itemScalePitch", itemScalePitch.toString())
        props.setProperty("itemScaleRoll", itemScaleRoll.toString())
        props.setProperty("itemScaleSpeed", itemScaleSpeed.toString())

        props.setProperty("ignoreHaste", ignoreHaste.toString())
        props.setProperty("noEquipReset", noEquipReset.toString())
        props.setProperty("noSwing", noSwing.toString())
        props.setProperty("noTerminatorSwing", noTerminatorSwing.toString())
        
        props.setProperty("autoRequeue", autoRequeue.toString())
        props.setProperty("secretsSound", secretsSound.toString())
        props.setProperty("dungeonDeathsDisplay", dungeonDeathsDisplay.toString())
        
        props.setProperty("lividSolver", lividSolver.toString())
        props.setProperty("lividBoxColor", lividBoxColor.toString())
        props.setProperty("lividLineWidth", lividLineWidth.toString())
        props.setProperty("lividTracer", lividTracer.toString())
        props.setProperty("lividHideWrong", lividHideWrong.toString())

        try {
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            file.writer().use { props.store(it, "XenAddons Configuration") }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load() {
        if (!file.exists()) {
            save() // Create default file
            return
        }
        try {
            val props = Properties()
            file.reader().use { props.load(it) }

            itemScaleSize = props.getProperty("itemScaleSize", "0.0").toFloatOrNull() ?: 0f
            scaleSwing = props.getProperty("scaleSwing", "false").toBoolean()
            itemScaleX = props.getProperty("itemScaleX", "0.0").toFloatOrNull() ?: 0f
            itemScaleY = props.getProperty("itemScaleY", "0.0").toFloatOrNull() ?: 0f
            itemScaleZ = props.getProperty("itemScaleZ", "0.0").toFloatOrNull() ?: 0f
            itemScaleYaw = props.getProperty("itemScaleYaw", "0.0").toFloatOrNull() ?: 0f
            itemScalePitch = props.getProperty("itemScalePitch", "0.0").toFloatOrNull() ?: 0f
            itemScaleRoll = props.getProperty("itemScaleRoll", "0.0").toFloatOrNull() ?: 0f
            itemScaleSpeed = props.getProperty("itemScaleSpeed", "0.0").toFloatOrNull() ?: 0f

            ignoreHaste = props.getProperty("ignoreHaste", "false").toBoolean()
            noEquipReset = props.getProperty("noEquipReset", "false").toBoolean()
            noSwing = props.getProperty("noSwing", "false").toBoolean()
            noTerminatorSwing = props.getProperty("noTerminatorSwing", "false").toBoolean()

            autoRequeue = props.getProperty("autoRequeue", "false").toBoolean()
            secretsSound = props.getProperty("secretsSound", "false").toBoolean()
            dungeonDeathsDisplay = props.getProperty("dungeonDeathsDisplay", "false").toBoolean()
            
            lividSolver = props.getProperty("lividSolver", "false").toBoolean()
            lividBoxColor = props.getProperty("lividBoxColor", "65535").toIntOrNull() ?: 0x00FFFF
            lividLineWidth = props.getProperty("lividLineWidth", "3.0").toFloatOrNull() ?: 3f
            lividTracer = props.getProperty("lividTracer", "true").toBoolean()
            lividHideWrong = props.getProperty("lividHideWrong", "false").toBoolean()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset() {
        itemScaleSize = 0f
        scaleSwing = false
        itemScaleX = 0f
        itemScaleY = 0f
        itemScaleZ = 0f
        itemScaleYaw = 0f
        itemScalePitch = 0f
        itemScaleRoll = 0f
        itemScaleSpeed = 0f
        ignoreHaste = false
        noEquipReset = false
        noSwing = false
        noTerminatorSwing = false
        
        autoRequeue = false
        secretsSound = false
        dungeonDeathsDisplay = false
        
        lividSolver = false
        lividBoxColor = 0x00FFFF
        lividLineWidth = 3f
        lividTracer = false
        lividHideWrong = false
        
        save()
    }
}

package xenus.xenaddons

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import xenus.xenaddons.gui.XenAddonsScreen

object XenAddonsClient : ClientModInitializer {
    private val logger = org.slf4j.LoggerFactory.getLogger("xenaddons-client")
    private var screenToOpen: net.minecraft.client.gui.screens.Screen? = null

    override fun onInitializeClient() {
        xenus.xenaddons.config.XenAddonsConfig.load()
        logger.info("XenAddonsClient initialized!")

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (screenToOpen != null) {
                client.setScreen(screenToOpen)
                screenToOpen = null
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            logger.info("Registering /xa command")
            dispatcher.register(
                    ClientCommandManager.literal("xa").executes { context ->
                        logger.info("Command /xa executed!")
                        // Schedule opening for next tick
                        screenToOpen = XenAddonsScreen(null)
                        1
                    }
            )
        }
    }
}

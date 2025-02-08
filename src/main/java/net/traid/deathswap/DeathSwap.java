package net.traid.deathswap;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(DeathSwap.MOD_ID)
public class DeathSwap {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "deathswap";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public DeathSwap() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register Death Swap game mechanics and commands (directly registering via the event bus)
        MinecraftForge.EVENT_BUS.register(DeathSwapGame.class);  // Register the game mechanics
        MinecraftForge.EVENT_BUS.register(DeathSwapCommands.class);  // Register commands
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // This is the place for common setup code (e.g., initializing game mechanics)
        LOGGER.info("DeathSwap mod is loading.");
    }

    // This will be called when the server is starting
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("DeathSwap mod is starting.");
    }

    // Client-side setup (if necessary)
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Client-side setup (e.g., adding textures, UI elements)
        }
    }
}

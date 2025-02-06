package net.traid.deathswap;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DeathSwapCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("startswap")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2 (admin)
                .executes(context -> {
                    DeathSwapGame.startGame(context.getSource().getServer()); // Passes the MinecraftServer instance
                    return Command.SINGLE_SUCCESS;
                }));

        dispatcher.register(Commands.literal("stopswap")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2 (admin)
                .executes(context -> {
                    DeathSwapGame.stopGame();
                    return Command.SINGLE_SUCCESS;
                }));
    }
}

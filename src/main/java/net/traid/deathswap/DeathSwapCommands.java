package net.traid.deathswap;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DeathSwapCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("deathswap")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2 (admin)

                // Start Command
                .then(Commands.literal("start")
                        .executes(context -> {
                            DeathSwapGame.startGame(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal("§aDeath Swap has started!"), true);
                            return Command.SINGLE_SUCCESS;
                        }))

                // Stop Command
                .then(Commands.literal("stop")
                        .executes(context -> {
                            DeathSwapGame.stopGame();
                            context.getSource().sendSuccess(() -> Component.literal("§cDeath Swap has been stopped."), true);
                            return Command.SINGLE_SUCCESS;
                        }))

                // Configuration Commands
                .then(Commands.literal("config")
                        .then(Commands.literal("randomItemDrops")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean value = BoolArgumentType.getBool(context, "value");
                                            Config.setRandomItemDrops(value);
                                            context.getSource().sendSuccess(() -> Component.literal("§6Random item drops set to: " + value), true);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("randomBlockDrops")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean value = BoolArgumentType.getBool(context, "value");
                                            Config.setRandomBlockDrops(value);
                                            context.getSource().sendSuccess(() -> Component.literal("§6Random block drops set to: " + value), true);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("randomMobDrops")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean value = BoolArgumentType.getBool(context, "value");
                                            Config.setRandomMobDrops(value);
                                            context.getSource().sendSuccess(() -> Component.literal("§6Random mob drops set to: " + value), true);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("timeSet")
                                .then(Commands.argument("minTime", IntegerArgumentType.integer(10, 600))
                                        .then(Commands.argument("maxTime", IntegerArgumentType.integer(10, 600))
                                                .executes(context -> {
                                                    int minTime = IntegerArgumentType.getInteger(context, "minTime");
                                                    int maxTime = IntegerArgumentType.getInteger(context, "maxTime");
                                                    if (minTime > maxTime) {
                                                        context.getSource().sendFailure(Component.literal("§cMin time cannot be greater than max time."));
                                                        return 0;
                                                    }
                                                    Config.setSwapTime(minTime, maxTime);
                                                    context.getSource().sendSuccess(() -> Component.literal("§6Swap time set to: " + minTime + " - " + maxTime + " seconds"), true);
                                                    return Command.SINGLE_SUCCESS;
                                                }))))));
    }
}

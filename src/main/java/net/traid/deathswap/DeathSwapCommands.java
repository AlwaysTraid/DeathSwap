package net.traid.deathswap;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                                .executes(context -> {
                                    boolean value = Config.RANDOM_ITEM_DROPS.get(); // Get current config value
                                    context.getSource().sendSuccess(() -> Component.literal("§6Random item drops is currently: " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean value = BoolArgumentType.getBool(context, "value");
                                            Config.setRandomItemDrops(value);
                                            context.getSource().sendSuccess(() -> Component.literal("§6Random item drops set to: " + value), true);
                                            return Command.SINGLE_SUCCESS;
                                        })))

                        .then(Commands.literal("randomBlockDrops")
                                .executes(context -> {
                                    boolean value = Config.RANDOM_BLOCK_DROPS.get(); // Get current config value
                                    context.getSource().sendSuccess(() -> Component.literal("§6Random block drops is currently: " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean value = BoolArgumentType.getBool(context, "value");
                                            Config.setRandomBlockDrops(value);
                                            context.getSource().sendSuccess(() -> Component.literal("§6Random block drops set to: " + value), true);
                                            return Command.SINGLE_SUCCESS;
                                        })))

                        .then(Commands.literal("randomMobDrops")
                                .executes(context -> {
                                    boolean value = Config.RANDOM_MOB_DROPS.get(); // Get current config value
                                    context.getSource().sendSuccess(() -> Component.literal("§6Random mob drops is currently: " + value), true);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean value = BoolArgumentType.getBool(context, "value");
                                            Config.setRandomMobDrops(value);
                                            context.getSource().sendSuccess(() -> Component.literal("§6Random mob drops set to: " + value), true);
                                            return Command.SINGLE_SUCCESS;
                                        })))

                        .then(Commands.literal("timeSet")
                                // First, we handle the case where no time type is given, so we display a usage message
                                .executes(context -> {
                                    context.getSource().sendFailure(Component.literal("§cYou must specify 'swapTime' or 'itemTime'."));
                                    context.getSource().sendSuccess(() -> Component.literal("§6Usage: /deathswap config timeSet <swapTime/itemTime> [minTime] [maxTime]"), false);
                                    return Command.SINGLE_SUCCESS;
                                })
                                // Next, we handle the 'timeType' argument (swapTime or itemTime)
                                .then(Commands.argument("timeType", StringArgumentType.word())
                                        .executes(context -> {
                                            String timeType = StringArgumentType.getString(context, "timeType");

                                            if (timeType.equals("swapTime")) {
                                                int minTime = Config.MIN_SWAP_TIME.get();
                                                int maxTime = Config.MAX_SWAP_TIME.get();
                                                context.getSource().sendSuccess(() -> Component.literal("§6Swap time is: " + minTime + " - " + maxTime + " seconds"), true);
                                            } else if (timeType.equals("itemTime")) {
                                                int minTime = Config.MIN_ITEMDROP_TIME.get();
                                                int maxTime = Config.MAX_ITEMDROP_TIME.get();
                                                context.getSource().sendSuccess(() -> Component.literal("§6Item drop time is: " + minTime + " - " + maxTime + " seconds"), true);
                                            } else {
                                                context.getSource().sendFailure(Component.literal("§cInvalid time type! Use either 'swapTime' or 'itemTime'."));
                                                return 0;
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        // Then, handle the case where minTime and maxTime are provided to set new values
                                        .then(Commands.argument("minTime", IntegerArgumentType.integer(10, 600))
                                                .then(Commands.argument("maxTime", IntegerArgumentType.integer(10, 600))
                                                        .executes(context -> {
                                                            String timeType = StringArgumentType.getString(context, "timeType");
                                                            int minTime = IntegerArgumentType.getInteger(context, "minTime");
                                                            int maxTime = IntegerArgumentType.getInteger(context, "maxTime");

                                                            // Ensure minTime is not greater than maxTime
                                                            if (minTime > maxTime) {
                                                                context.getSource().sendFailure(Component.literal("§cMin time cannot be greater than max time."));
                                                                return 0;
                                                            }

                                                            // Set the time based on the 'timeType'
                                                            if (timeType.equals("swapTime")) {
                                                                Config.setSwapTime(minTime, maxTime); // Update swap time
                                                                context.getSource().sendSuccess(() -> Component.literal("§6Swap time set to: " + minTime + " - " + maxTime + " seconds"), true);
                                                            } else if (timeType.equals("itemTime")) {
                                                                Config.setItemDropTime(minTime, maxTime); // Update item drop time
                                                                context.getSource().sendSuccess(() -> Component.literal("§6Item drop time set to: " + minTime + " - " + maxTime + " seconds"), true);
                                                            } else {
                                                                context.getSource().sendFailure(Component.literal("§cInvalid time type! Use either 'swapTime' or 'itemTime'."));
                                                                return 0;
                                                            }
                                                            return Command.SINGLE_SUCCESS;
                                                        })))))));
    }


}

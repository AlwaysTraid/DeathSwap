package net.traid.deathswap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class DeathSwapGame {
    private static boolean gameRunning = false;
    private static int timeLeft = 600; // 30 seconds (20 ticks per second)
    private static final Random random = new Random();
    private static MinecraftServer serverInstance;

    public static void startGame(MinecraftServer server) {
        gameRunning = true;
        serverInstance = server;
        timeLeft = 600 + random.nextInt(600); // Random between 30-60 seconds

        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            // Send the main title (big text in center)
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§aGood Luck")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Has Started!")));

            // Play the sound effect
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        //For in chat notifications
        //server.getPlayerList().broadcastSystemMessage(Component.literal("§aDeath Swap has started!"), false);
    }

    public static void stopGame() {
        gameRunning = false;
        if (serverInstance != null) {
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                // Send the main title (big text in center)
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§cGame Over")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Is Over!")));

                // Play the sound effect
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            //For In Chat Notifications
            //serverInstance.getPlayerList().broadcastSystemMessage(Component.literal("§cDeath Swap has been stopped."), false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!gameRunning) return;
        timeLeft--;

        // Display countdown & play ding sound for last 5 seconds
        if (timeLeft <= 100 && timeLeft % 20 == 0) { // Every second for last 5 seconds
            int secondsLeft = timeLeft / 20;
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                // Send the main title (big text in center)
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e" + secondsLeft)));

                // Send the subtitle (small text below the title)
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Get ready to swap!")));

                // Play the sound effect
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }


        // Swap players when time reaches 0
        if (timeLeft <= 0) {
            swapPlayers(serverInstance);
            timeLeft = 600 + random.nextInt(600);
        }
    }

    private static void swapPlayers(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        if (players.size() < 2) {
            stopGame();
            return;
        }

        Collections.shuffle(players);
        List<Vec3> positions = new ArrayList<>();
        for (ServerPlayer player : players) {
            positions.add(player.position());
        }

        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            Vec3 newPos = positions.get((i + 1) % players.size());
            ServerLevel level = player.serverLevel();
            player.teleportTo(level, newPos.x, newPos.y, newPos.z, player.getYRot(), player.getXRot());
            player.sendSystemMessage(Component.literal("§cSwapped!"));
        }

        server.getPlayerList().broadcastSystemMessage(Component.literal("§eDeath Swap has happened!"), false);
    }
}

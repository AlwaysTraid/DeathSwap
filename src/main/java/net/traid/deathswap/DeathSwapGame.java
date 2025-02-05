package net.traid.deathswap;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class DeathSwapGame {
    private static boolean gameRunning = false;
    private static int timeLeft = 600; // 30 seconds (20 ticks per second)
    private static final Random random = new Random();
    private static MinecraftServer serverInstance;
    private static final List<ServerPlayer> eliminatedPlayers = new ArrayList<>();

    public static void startGame(MinecraftServer server) {
        gameRunning = true;
        serverInstance = server;
        timeLeft = 600 + random.nextInt(600); // Random between 30-60 seconds

        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            // Send the main title (big text in center)
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§aGood Luck")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Has Started!")));
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void stopGame() {
        gameRunning = false;
        if (serverInstance != null) {
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§cGame Over")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Is Over!")));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!gameRunning) return;
        timeLeft--;

        if (timeLeft <= 100 && timeLeft % 20 == 0) { // Last 5 seconds
            int secondsLeft = timeLeft / 20;
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e" + secondsLeft)));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Get ready to swap!")));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        if (timeLeft <= 0) {
            swapPlayers(serverInstance);
            timeLeft = 600 + random.nextInt(600);
        }

        // Check for game end condition: only one player remains
        if (serverInstance.getPlayerList().getPlayers().size() - eliminatedPlayers.size() <= 1) {
            stopGame();
            announceWinner();
        }
    }

    private static void swapPlayers(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        players.removeAll(eliminatedPlayers); // Remove eliminated players

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

    public static void eliminatePlayer(ServerPlayer player) {
        if (eliminatedPlayers.contains(player)) return; // Player is already eliminated

        eliminatedPlayers.add(player);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§cYou Have Been Eliminated")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6You are now a spectator")));
        player.setGameMode(GameType.SPECTATOR); // Set player to spectator mode
        player.sendSystemMessage(Component.literal("§cYou have been eliminated and are now a spectator."));

        serverInstance.getPlayerList().broadcastSystemMessage(Component.literal("§e" + player.getName().getString() + " has been eliminated!"), false);
    }

    private static void announceWinner() {
        if (serverInstance == null) return;
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            if (!eliminatedPlayers.contains(player)) {
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§aYou Won!")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Winner")));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // If the player is eliminated
        if (gameRunning && !eliminatedPlayers.contains(player)) {
            eliminatePlayer(player);
        }
    }
}

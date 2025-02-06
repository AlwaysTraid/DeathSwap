package net.traid.deathswap;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
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
    private static MinecraftServer serverInstance;
    private static final List<ServerPlayer> eliminatedPlayers = new ArrayList<>();
    private static final int MIN_SWAP_INTERVAL = 600; // 30 seconds in ticks
    private static final int MAX_SWAP_INTERVAL = 3600; // 3 minutes in ticks
    private static int swapTimer;

    public static void startGame(MinecraftServer server) {
        gameRunning = true;
        serverInstance = server;
        swapTimer = randomSwapInterval(); // Initialize with a random swap interval

        DeathSwapItemDrops.startItemDrops(server); // Item Drops Start

        // Set all players to Survival mode
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            player.setGameMode(GameType.SURVIVAL); // Set each player to Survival
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§aGood Luck")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Has Started!")));
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void stopGame() {
        gameRunning = false;
        if (serverInstance != null) {
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                // Ensure all players are set to Survival mode when the game ends
                player.setGameMode(GameType.SURVIVAL); // Set each player to Survival mode at the end of the game
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§cGame Over")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Is Over!")));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public static boolean isGameRunning() {
        return gameRunning;
    }

    private static String generateProgressBar(float progress) {
        int totalBars = 20;
        int filledBars = (int) (totalBars * progress);
        StringBuilder bar = new StringBuilder("§e[");
        for (int i = 0; i < totalBars; i++) {
            bar.append(i < filledBars ? "§a|" : "§7|");
        }
        bar.append("§e]");
        return bar.toString();
    }

    // Randomly select a new swap interval between 30 seconds (600 ticks) and 3 minutes (3600 ticks)
    private static int randomSwapInterval() {
        Random rand = new Random();
        return rand.nextInt(MAX_SWAP_INTERVAL - MIN_SWAP_INTERVAL + 1) + MIN_SWAP_INTERVAL;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!gameRunning) return;

        swapTimer--; // Decrease the swap timer by 1 each tick

        // Update action bar for all players
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            float progress = (float) swapTimer / MAX_SWAP_INTERVAL;
            String progressBar = generateProgressBar(progress);
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§6Swap In: " + progressBar)));
        }

        // Countdown in last 5 seconds
        if (swapTimer <= 100 && swapTimer % 20 == 0) { // Last 5 seconds (100 ticks)
            int secondsLeft = swapTimer / 20;
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e" + secondsLeft)));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Get ready to swap!")));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        // Swap players when the timer hits 0
        if (swapTimer <= 0) {
            swapPlayers(serverInstance);
            swapTimer = randomSwapInterval(); // Set a new random interval for the next swap
        }

        // Check if only one player remains
        List<ServerPlayer> alivePlayers = new ArrayList<>(serverInstance.getPlayerList().getPlayers());
        alivePlayers.removeIf(p -> p.isSpectator() || eliminatedPlayers.contains(p));

        if (alivePlayers.size() <= 1) {
            stopGame();
            announceWinner();
        }
    }

    private static void swapPlayers(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        players.removeIf(p -> p.isSpectator() || eliminatedPlayers.contains(p));

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

            // Ensure safe teleport (optional)
            // if (level.getBlockState(newPos.below()).isAir()) {
            //     newPos = new Vec3(newPos.x, level.getHeight(), newPos.z); // Move player to highest block
            // }

            player.teleportTo(level, newPos.x, newPos.y, newPos.z, player.getYRot(), player.getXRot());
            player.sendSystemMessage(Component.literal("§cSwapped!"));
        }

        server.getPlayerList().broadcastSystemMessage(Component.literal("§eDeath Swap has happened!"), false);
    }

    public static void eliminatePlayer(ServerPlayer player) {
        if (eliminatedPlayers.contains(player)) return;

        eliminatedPlayers.add(player);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§cYou Have Been Eliminated")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6You are now a spectator")));
        player.setGameMode(GameType.SPECTATOR);
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
        if (!gameRunning || !(event.getEntity() instanceof ServerPlayer player)) return;
        eliminatePlayer(player);
    }
}

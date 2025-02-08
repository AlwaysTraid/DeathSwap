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
    private static int swapTimer;

    public static void startGame(MinecraftServer server) {
        gameRunning = true;
        serverInstance = server;
        swapTimer = randomSwapInterval();

        DeathSwapItemDrops.startItemDrops(server);

        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            player.setGameMode(GameType.SURVIVAL);
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§aGood Luck")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Has Started!")));
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void stopGame() {
        gameRunning = false;
        if (serverInstance != null) {
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                player.setGameMode(GameType.SURVIVAL);
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§cGame Over")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Death Swap Is Over!")));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public static boolean isGameRunning() {
        return gameRunning;
    }

    private static int randomSwapInterval() {
        Random rand = new Random();
        return (rand.nextInt(Config.MAX_SWAP_TIME.get() - Config.MIN_SWAP_TIME.get() + 1) + Config.MIN_SWAP_TIME.get()) * 20;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!gameRunning) return;

        swapTimer--;
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§6Swap In: " + (swapTimer / 20) + "s")));
        }

        if (swapTimer <= 100 && swapTimer % 20 == 0) {
            int secondsLeft = swapTimer / 20;
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e" + secondsLeft)));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§6Get ready to swap!")));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        if (swapTimer <= 0) {
            swapPlayers(serverInstance);
            swapTimer = randomSwapInterval();
            System.out.println("Random Swap Interval: " + swapTimer);
        }

//        // Log the remaining time to swap in the console
//        int secondsLeftToSwap = swapTimer / 20;
//        System.out.println("Time left to swap: " +  (swapTimer / 20) + " seconds");
        System.out.println("Config.MAX_SWAP_TIME: " + Config.MAX_SWAP_TIME.get());
        System.out.println("Config.Min_SWAP_TIME: " + Config.MIN_SWAP_TIME.get());
        System.out.println("Random Swap Interval: " + swapTimer);

        List<ServerPlayer> alivePlayers = new ArrayList<>(serverInstance.getPlayerList().getPlayers());
        alivePlayers.removeIf(p -> p.isSpectator() || eliminatedPlayers.contains(p));
        if (alivePlayers.size() <= 0) {
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

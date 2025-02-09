package net.traid.deathswap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class DeathSwapItemDrops {
    private static final Random random = new Random();
    private static int itemDropTimer;  // Timer in milliseconds
    private static int itemStartTimer;
    private static long lastUpdateTime = 0; // Track last time update occurred
    private static final int ITEM_DROP_INTERVAL_MS = 1000; // 30 seconds in milliseconds
    private static MinecraftServer serverInstance;
    private static boolean randomItemDrops = true;
    private static boolean randomBlockDrops = false;
    private static boolean randomMobDrops = false;

    public static void startItemDrops(MinecraftServer server) {
        if (serverInstance == null) {
            serverInstance = server;
        }
        itemDropTimer = randomItemDropInterval();  // Timer in milliseconds
        itemStartTimer = itemDropTimer;
        lastUpdateTime = 0; // Track last time update occurred
        randomBlockDrops = Config.RANDOM_BLOCK_DROPS.get();
        randomMobDrops = Config.RANDOM_MOB_DROPS.get();
    }

    private static int randomItemDropInterval() {
        int minItemDropTime = Config.MIN_ITEMDROP_TIME.get(); // in seconds
        int maxItemDropTime = Config.MAX_ITEMDROP_TIME.get(); // in seconds

        Random rand = new Random();
        // Return a random value between min and max item drop time in ticks
        return (rand.nextInt(maxItemDropTime - minItemDropTime + 1) + minItemDropTime) * 20; // 20 ticks per second
    }

    private static final int ITEM_INTERVAL_MS = 1000; // 1000 ms = 1 second

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (serverInstance == null || !DeathSwapGame.isGameRunning() || !randomItemDrops) return;

        // Get the current time in milliseconds
        long currentTime = System.currentTimeMillis();

        // If 1 second has passed (every 1000 ms)
        if (currentTime - lastUpdateTime >= ITEM_INTERVAL_MS) {
            lastUpdateTime = currentTime; // Update last update time

            // Decrease swapTimer by 1 every second
            itemDropTimer -= 20;

            // Every 20 ticks = 1 second
            if (itemDropTimer <= 0) {
                // Give items to players
                giveRandomItems();
                itemDropTimer = randomItemDropInterval(); // Reset the timer to a random value in ticks
                itemStartTimer = itemDropTimer;
            }


            // Optionally: Update the progress bar with the current item drop progress
            float progress = (float) itemDropTimer / itemStartTimer;
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                    if (progress > 0)
                        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§6Item Drop: " + generateProgressBar(progress))));
                }
            }
        }

    }

    private static void giveRandomItems() {
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;

            Item randomItem = getRandomItem();
            ItemStack itemStack = new ItemStack(randomItem, 1);

            player.sendSystemMessage(Component.literal("§aYou received: ").append(itemStack.getHoverName()));

            if (!player.getInventory().add(itemStack)) {
                player.drop(itemStack, false);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!randomBlockDrops || event.getPlayer().level().isClientSide) return;

        event.setCanceled(true); // Prevent normal drop

        ServerPlayer player = (ServerPlayer) event.getPlayer();

        // Destroy the block at the given position (simulate block breaking)
        player.level().destroyBlock(event.getPos(), false);

        // Create a random item drop
        ItemStack randomDrop = new ItemStack(getRandomItem(), 1);

        // Drop the item at the same position where the block was destroyed
        player.level().addFreshEntity(new ItemEntity(player.level(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), randomDrop));
    }


    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!randomMobDrops || !(event.getEntity() instanceof LivingEntity) || event.getEntity().level().isClientSide) return;

        LivingEntity entity = (LivingEntity) event.getEntity();
        ItemStack randomDrop = new ItemStack(getRandomItem(), 1);
        entity.level().addFreshEntity(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), randomDrop));
    }

    private static Item getRandomItem() {
        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR) // Ensure AIR is never selected
                .toList();

        Item selectedItem = items.get(random.nextInt(items.size()));

        return selectedItem;
    }

    private static String generateProgressBar(float progress) {
        int totalBars = 20;
        int filledBars = (int) (totalBars * progress);
        return "§e[" + "§a|".repeat(filledBars) + "§7|".repeat(totalBars - filledBars) + "§e]";
    }
}

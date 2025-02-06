package net.traid.deathswap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class DeathSwapItemDrops {
    private static final Random random = new Random();
    private static int itemDropTimer = 600; // 30s in ticks
    private static final int ITEM_DROP_INTERVAL = 600; // 30s
    private static MinecraftServer serverInstance;

    public static void startItemDrops(MinecraftServer server) {
        if (serverInstance == null) { // Ensure it's only set once
            serverInstance = server;
        }
        itemDropTimer = ITEM_DROP_INTERVAL;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (serverInstance == null || !DeathSwapGame.isGameRunning()) return;

        itemDropTimer--;

        // Update action bar for players
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) { // Skip eliminated players
                float progress = (float) itemDropTimer / ITEM_DROP_INTERVAL;
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§6Item Drop: " + generateProgressBar(progress))));
            }
        }

        // Drop items when the timer reaches 0
        if (itemDropTimer <= 0) {
            giveRandomItems();
            itemDropTimer = ITEM_DROP_INTERVAL;
        }
    }

    private static void giveRandomItems() {
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;

            Item randomItem = getRandomItem();
            ItemStack itemStack = new ItemStack(randomItem, 1);


//            System.out.println("Actual given item: " + BuiltInRegistries.ITEM.getKey(randomItem));
//            System.out.println("Display name: " + itemStack.getHoverName().getString());


            if (!player.getInventory().add(itemStack)) {
                player.drop(itemStack, false);
            }


//            player.sendSystemMessage(Component.literal("§aYou received: ").append(itemStack.getHoverName().getString()));
        }
    }

    private static Item getRandomItem() {
        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR) // Ensure AIR is never selected
                .toList();

        Item selectedItem = items.get(random.nextInt(items.size()));

        // Debugging message
        System.out.println("Randomly selected item: " + BuiltInRegistries.ITEM.getKey(selectedItem));

        return selectedItem;
    }

    private static String generateProgressBar(float progress) {
        int totalBars = 20;
        int filledBars = (int) (totalBars * progress);
        return "§e[" + "§a|".repeat(filledBars) + "§7|".repeat(totalBars - filledBars) + "§e]";
    }
}

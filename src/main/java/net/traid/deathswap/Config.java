package net.traid.deathswap;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue RANDOM_ITEM_DROPS;
    public static final ForgeConfigSpec.BooleanValue RANDOM_BLOCK_DROPS;
    public static final ForgeConfigSpec.BooleanValue RANDOM_MOB_DROPS;
    public static final ForgeConfigSpec.IntValue MIN_SWAP_TIME;
    public static final ForgeConfigSpec.IntValue MAX_SWAP_TIME;

    static {
        BUILDER.push("DeathSwap Settings");

        // Config option for random item drops (default: true)
        RANDOM_ITEM_DROPS = BUILDER.comment("Enable random items dropping when players swap.")
                .define("randomItemDrops", true);

        // Config option for random drops from blocks when broken (default: false)
        RANDOM_BLOCK_DROPS = BUILDER.comment("Enable random drops from blocks when broken.")
                .define("randomBlockDrops", false);

        // Config option for random drops from mobs when killed (default: false)
        RANDOM_MOB_DROPS = BUILDER.comment("Enable random drops from mobs when killed.")
                .define("randomMobDrops", false);

        // Config option for the minimum swap time range (default: 60 seconds)
        MIN_SWAP_TIME = BUILDER.comment("Minimum swap time in seconds (default: 60).")
                .defineInRange("minSwapTime", 60, 10, 600);

        // Config option for the maximum swap time range (default: 300 seconds)
        MAX_SWAP_TIME = BUILDER.comment("Maximum swap time in seconds (default: 300).")
                .defineInRange("maxSwapTime", 300, 10, 600);

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Method to update the random item drops setting
    public static void setRandomItemDrops(boolean enabled) {
        RANDOM_ITEM_DROPS.set(enabled);
    }

    // Method to update the random block drops setting
    public static void setRandomBlockDrops(boolean enabled) {
        RANDOM_BLOCK_DROPS.set(enabled);
    }

    // Method to update the random mob drops setting
    public static void setRandomMobDrops(boolean enabled) {
        RANDOM_MOB_DROPS.set(enabled);
    }

    // Method to update the swap time range
    public static void setSwapTime(int minTime, int maxTime) {
        MIN_SWAP_TIME.set(minTime);
        MAX_SWAP_TIME.set(maxTime);
    }
}

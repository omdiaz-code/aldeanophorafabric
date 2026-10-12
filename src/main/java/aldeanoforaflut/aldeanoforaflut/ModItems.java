package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public class ModItems {
    public static final Item DILITIO = new DilitioItem(new Item.Properties().stacksTo(64));
    public static final Item PHORA_TABLET = new PhoraTabletItem(new Item.Properties().stacksTo(1));
    public static final Item COPPER_KEY = new CopperKeyItem(new Item.Properties().stacksTo(1));

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Aldeanoforaflut.MODID, "dilitio"), DILITIO);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Aldeanoforaflut.MODID, "phora_tablet"), PHORA_TABLET);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Aldeanoforaflut.MODID, "copper_key"), COPPER_KEY);
    }

    public static boolean hasCopperKey(Player player) {
        if (player == null) return false;
        if (player.getOffhandItem().is(COPPER_KEY)) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(COPPER_KEY)) return true;
        }
        return false;
    }

    /** Consume exactly one key when a Phora reset succeeds. */
    public static boolean consumeCopperKey(Player player) {
        if (player == null) return false;

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(COPPER_KEY)) {
            return consumeOneAndSync(player, offhand);
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(COPPER_KEY)) {
                return consumeOneAndSync(player, stack);
            }
        }
        return false;
    }

    /** Consume the actual key used for a hand interaction. */
    public static boolean consumeCopperKey(Player player, InteractionHand hand) {
        if (player == null) return false;
        ItemStack held = player.getItemInHand(hand);
        return held.is(COPPER_KEY) && consumeOneAndSync(player, held);
    }

    private static boolean consumeOneAndSync(Player player, ItemStack stack) {
        if (!stack.is(COPPER_KEY)) return false;
        stack.shrink(1);
        player.getInventory().setChanged();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.broadcastChanges();
        }
        return true;
    }
}

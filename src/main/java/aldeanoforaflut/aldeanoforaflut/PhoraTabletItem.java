package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PhoraTabletItem extends Item {

    public PhoraTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            if (player.hasPermissions(2)) {
                TradeConfigData data = TradeConfigData.get((ServerLevel) level);
                CompoundTag tag = new CompoundTag();
                data.save(tag);
                ModNetworking.sendOpenTradeConfig((ServerPlayer) player, tag);
            } else {
                player.sendSystemMessage(Component.literal("§cSolo administradores pueden usar la Tablet Phora."));
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}

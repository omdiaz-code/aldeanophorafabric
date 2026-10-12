package aldeanoforaflut.aldeanoforaflut;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class ModNetworking {
    public static final ResourceLocation OPEN_TRADE_CONFIG = id("open_trade_config");
    public static final ResourceLocation SAVE_TRADE_CONFIG = id("save_trade_config");
    public static final ResourceLocation PHORA_CYCLE_ACCESS = id("phora_cycle_access");
    public static final ResourceLocation PHORA_TRADE_INFO = id("phora_trade_info");
    public static final ResourceLocation PHORA_RESET = id("phora_reset");

    private static final double MAX_RESET_DISTANCE_SQR = 8.0 * 8.0;

    private ModNetworking() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Aldeanoforaflut.MODID, path);
    }

    public static void registerServerMessages() {
        ServerPlayNetworking.registerGlobalReceiver(PHORA_CYCLE_ACCESS, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            server.execute(() -> {
                Entity entity = player.level().getEntity(entityId);
                if (entity instanceof TransformableMerchantEntity phora && phora.isOwner(player)) {
                    phora.cycleAccessMode();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SAVE_TRADE_CONFIG, (server, player, handler, buf, responseSender) -> {
            CompoundTag data = buf.readNbt();
            server.execute(() -> {
                if (data == null || !player.hasPermissions(2)) {
                    return;
                }
                TradeConfigData.get(player.serverLevel()).loadFromClient(data);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PHORA_RESET, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            server.execute(() -> {
                Entity entity = player.level().getEntity(entityId);
                if (entity instanceof TransformableMerchantEntity phora
                        && phora.isActive()
                        && player.distanceToSqr(phora) <= MAX_RESET_DISTANCE_SQR
                        && (phora.isOwner(player) || player.hasPermissions(2))
                        && ModItems.consumeCopperKey(player)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    phora.resetToBlock();
                }
            });
        });
    }

    public static void sendTradeInfo(ServerPlayer player, int containerId, int entityId,
                                     long passiveTicks, int accessMode, boolean isOwner, int[] offerLevels) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(containerId);
        buf.writeVarInt(entityId);
        buf.writeVarLong(passiveTicks);
        buf.writeVarInt(accessMode);
        buf.writeBoolean(isOwner);
        buf.writeVarIntArray(offerLevels);
        ServerPlayNetworking.send(player, PHORA_TRADE_INFO, buf);
    }

    public static void sendOpenTradeConfig(ServerPlayer player, CompoundTag data) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(data);
        ServerPlayNetworking.send(player, OPEN_TRADE_CONFIG, buf);
    }
}

package aldeanoforaflut.aldeanoforaflut.entity.client;

import aldeanoforaflut.aldeanoforaflut.ModNetworking;
import aldeanoforaflut.aldeanoforaflut.PhoraTradeInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_TRADE_CONFIG, (client, handler, buf, responseSender) -> {
            CompoundTag data = buf.readNbt();
            if (data == null) {
                data = new CompoundTag();
            }
            CompoundTag configData = data;
            client.execute(() -> ClientPacketHandler.handleOpenTradeConfig(configData));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.PHORA_TRADE_INFO, (client, handler, buf, responseSender) -> {
            int containerId = buf.readVarInt();
            int entityId = buf.readVarInt();
            long passiveTicks = buf.readVarLong();
            int accessMode = buf.readVarInt();
            boolean isOwner = buf.readBoolean();
            int[] offerLevels = buf.readVarIntArray();
            PhoraTradeInfo info = new PhoraTradeInfo(containerId, entityId, passiveTicks, accessMode, isOwner, offerLevels);
            client.execute(() -> ClientPacketHandler.handlePhoraTradeInfo(info));
        });
    }

    public static void sendSaveTradeConfig(CompoundTag data) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(data);
        ClientPlayNetworking.send(ModNetworking.SAVE_TRADE_CONFIG, buf);
    }

    public static void sendCycleAccess(int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        ClientPlayNetworking.send(ModNetworking.PHORA_CYCLE_ACCESS, buf);
    }

    public static void sendReset(int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        ClientPlayNetworking.send(ModNetworking.PHORA_RESET, buf);
    }
}
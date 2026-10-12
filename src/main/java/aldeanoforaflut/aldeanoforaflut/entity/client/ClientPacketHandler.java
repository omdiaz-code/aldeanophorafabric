package aldeanoforaflut.aldeanoforaflut.entity.client;

import aldeanoforaflut.aldeanoforaflut.PhoraTradeInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class ClientPacketHandler {
    private static PhoraTradeInfo lastTradeInfo;

    private ClientPacketHandler() {
    }

    public static void handleOpenTradeConfig(CompoundTag configData) {
        Minecraft.getInstance().setScreen(new TradeConfigScreen(configData));
    }

    public static void handlePhoraTradeInfo(PhoraTradeInfo packet) {
        lastTradeInfo = packet;
    }

    public static PhoraTradeInfo getTradeInfo(int containerId) {
        return lastTradeInfo != null && lastTradeInfo.containerId() == containerId ? lastTradeInfo : null;
    }
}
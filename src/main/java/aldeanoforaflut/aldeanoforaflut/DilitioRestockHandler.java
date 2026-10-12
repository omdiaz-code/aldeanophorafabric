package aldeanoforaflut.aldeanoforaflut;

import net.minecraft.world.item.trading.MerchantOffers;

public final class DilitioRestockHandler {
    public static final long TICKS_PER_DAY = 24_000L;
    public static final int CHECK_INTERVAL_TICKS = 20;

    private DilitioRestockHandler() {
    }

    public static void tick(TransformableMerchantEntity merchant) {
        if (merchant.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        long currentDay = merchant.level().getDayTime() / TICKS_PER_DAY;
        if (merchant.getLastRestockDay() == Long.MIN_VALUE) {
            merchant.setLastRestockDay(currentDay);
            return;
        }

        if (merchant.getLastRestockDay() == currentDay || merchant.getTradingPlayer() != null) {
            return;
        }

        merchant.setLastRestockDay(currentDay);
        MerchantOffers offers = merchant.getOffers();
        if (DilitioTrades.restock(offers, merchant::isOfferUnlocked)) {
            merchant.playSound(ModSounds.PHORA_RESTOCK, ModSounds.PHORA_VOLUME, 1.0F);
        }
    }
}
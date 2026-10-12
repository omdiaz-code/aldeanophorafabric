package aldeanoforaflut.aldeanoforaflut;


import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.function.IntPredicate;

public final class DilitioTrades {

    public static final int TRADES_PER_LEVEL = 2;
    public static final int MAX_USES = 12;

    private DilitioTrades() {}

    /**
     * Reinicia los usos de las ofertas usadas cuyo índice cumpla {@code canRestock}.
     * Las ofertas bloqueadas por nivel deben quedar fuera para no desbloquearse.
     */
    public static boolean restock(MerchantOffers offers, IntPredicate canRestock) {
        boolean restocked = false;
        if (offers == null) return false;
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer.getUses() > 0 && canRestock.test(i)) {
                offer.resetUses();
                restocked = true;
            }
        }
        return restocked;
    }

    /**
     * Agrega hasta {@value #TRADES_PER_LEVEL} ofertas del nivel indicado y devuelve cuántas agregó.
     * El objeto y la cantidad de salida se toman de la configuración de cada trade.
     */
    public static int addTradesForLevel(MerchantOffers offers, RandomSource random, int tradeLevel, int currentVillagerLevel, ServerLevel serverLevel) {
        TradeConfigData config = TradeConfigData.get(serverLevel);
        List<TradeConfigData.TradeEntry> availableTrades = config.getEntriesForLevel(tradeLevel);

        int added = 0;
        int tradesToAdd = Math.min(TRADES_PER_LEVEL, availableTrades.size());
        for (int i = 0; i < tradesToAdd; i++) {
            TradeConfigData.TradeEntry chosen = pickWeighted(availableTrades, random);
            if (chosen == null) break;
            availableTrades.remove(chosen); // Evita trades repetidos en el mismo nivel

            int count = chosen.minCount;
            if (chosen.maxCount > chosen.minCount) {
                count = chosen.minCount + random.nextInt((chosen.maxCount - chosen.minCount) + 1);
            }

            Item item = chosen.getItem();
            if (item == null || item == Items.AIR) item = Items.DIRT; // Fallback

            MerchantOffer newOffer = new MerchantOffer(
                    new ItemStack(item, count),
                    new ItemStack(chosen.getResultItem(), chosen.resultCount),
                    MAX_USES, getXpReward(tradeLevel), 0.05f
            );

            // Si el nivel requerido es mayor al actual, se marca como agotado para bloquearlo
            if (tradeLevel > currentVillagerLevel) {
                for (int j = 0; j < MAX_USES; j++) {
                    newOffer.increaseUses();
                }
            }

            offers.add(newOffer);
            added++;
        }
        return added;
    }

    private static TradeConfigData.TradeEntry pickWeighted(List<TradeConfigData.TradeEntry> entries, RandomSource random) {
        int totalWeight = entries.stream().mapToInt(e -> e.weight).sum();
        if (totalWeight <= 0) return null;

        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (TradeConfigData.TradeEntry entry : entries) {
            currentWeight += entry.weight;
            if (randomWeight < currentWeight) {
                return entry;
            }
        }
        return null;
    }

    private static int getXpReward(int level) {
        return switch (level) {
            case 1 -> 2;
            case 2 -> 10;
            case 3 -> 20;
            case 4 -> 30;
            case 5 -> 0; // Max level
            default -> 2;
        };
    }
}

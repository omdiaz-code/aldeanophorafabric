package aldeanoforaflut.aldeanoforaflut;

public record PhoraTradeInfo(int containerId, int entityId, long passiveTicks,
                             int accessMode, boolean isOwner, int[] offerLevels) {
    public int getOfferLevel(int index) {
        return index >= 0 && index < offerLevels.length ? offerLevels[index] : 0;
    }
}
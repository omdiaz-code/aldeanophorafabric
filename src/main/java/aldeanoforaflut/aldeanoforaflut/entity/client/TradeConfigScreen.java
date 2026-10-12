package aldeanoforaflut.aldeanoforaflut.entity.client;

import aldeanoforaflut.aldeanoforaflut.TradeConfigData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class TradeConfigScreen extends Screen {

    private static final int NODE_WIDTH = 160;
    private static final int NODE_HEIGHT = 36;
    private static final int COLUMN_SPACING = 40;
    private static final int NODE_SPACING = 10;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.0;

    private final List<TradeConfigData.TradeEntry> entries = new ArrayList<>();
    private double scrollX = 50;
    private double scrollY = 78;
    private double zoom = 1.0;
    private double zoomPanX;
    private double zoomPanY;

    public TradeConfigScreen(CompoundTag configData) {
        super(Component.literal("Trade Configuration"));
        loadFromTag(configData);
    }

    private void loadFromTag(CompoundTag tag) {
        entries.clear();
        ListTag list = tag.getList("Trades", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            entries.add(TradeConfigData.TradeEntry.load(list.getCompound(i)));
        }
    }

    private void saveAndSend() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (TradeConfigData.TradeEntry entry : entries) {
            list.add(entry.save());
        }
        tag.put("Trades", list);
        ClientNetworking.sendSaveTradeConfig(tag);
    }

    private double toCanvasX(double screenX) {
        double centerX = this.width / 2.0;
        return centerX + (screenX - centerX - zoomPanX) / zoom;
    }

    private double toCanvasY(double screenY) {
        double centerY = this.height / 2.0;
        return centerY + (screenY - centerY - zoomPanY) / zoom;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        this.scrollX += dragX / zoom;
        this.scrollY += dragY / zoom;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0) return false;

        double oldZoom = zoom;
        double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, oldZoom * Math.pow(1.12, delta)));
        if (newZoom == oldZoom) return true;

        // Keep the point under the mouse fixed while changing the scale.
        double centerX = this.width / 2.0;
        double centerY = this.height / 2.0;
        double fromCenterX = mouseX - centerX;
        double fromCenterY = mouseY - centerY;
        zoomPanX = fromCenterX - (fromCenterX - zoomPanX) * (newZoom / oldZoom);
        zoomPanY = fromCenterY - (fromCenterY - zoomPanY) * (newZoom / oldZoom);
        zoom = newZoom;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double canvasMouseX = toCanvasX(mouseX);
        double canvasMouseY = toCanvasY(mouseY);

        for (int level = 1; level <= 5; level++) {
            int levelX = (int) scrollX + (level - 1) * (NODE_WIDTH + COLUMN_SPACING);
            int levelY = (int) scrollY;

            // Agrega el objeto de la mano secundaria como costo de una oferta.
            if (canvasMouseX >= levelX && canvasMouseX <= levelX + NODE_WIDTH
                    && canvasMouseY >= levelY - 20 && canvasMouseY <= levelY - 5) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    ItemStack offhand = this.minecraft.player.getOffhandItem();
                    if (!offhand.isEmpty()) {
                        String itemId = BuiltInRegistries.ITEM.getKey(offhand.getItem()).toString();
                        entries.add(new TradeConfigData.TradeEntry(itemId, 1, offhand.getCount(), 10, level));
                        saveAndSend();
                        return true;
                    }
                }
            }

            int nodeIndex = 0;
            for (int i = 0; i < entries.size(); i++) {
                TradeConfigData.TradeEntry entry = entries.get(i);
                if (entry.requiredLevel != level) continue;

                int nx = levelX;
                int ny = levelY + nodeIndex * (NODE_HEIGHT + NODE_SPACING);

                // Clic en el resultado: copia el objeto y la cantidad de la mano secundaria.
                if (button == 0 && isHovering(canvasMouseX, canvasMouseY, nx + 88, ny + 8, 20, 20)) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        ItemStack offhand = this.minecraft.player.getOffhandItem();
                        if (!offhand.isEmpty()) {
                            entry.resultItemId = BuiltInRegistries.ITEM.getKey(offhand.getItem()).toString();
                            entry.resultCount = Math.min(offhand.getCount(), offhand.getMaxStackSize());
                            saveAndSend();
                        }
                    }
                    return true;
                }

                if (isHovering(canvasMouseX, canvasMouseY, nx + 108, ny + 19, 14, 12)) {
                    entry.weight = Math.max(1, entry.weight - 1);
                    saveAndSend();
                    return true;
                }
                if (isHovering(canvasMouseX, canvasMouseY, nx + 125, ny + 19, 14, 12)) {
                    entry.weight = Math.min(100, entry.weight + 1);
                    saveAndSend();
                    return true;
                }
                if (isHovering(canvasMouseX, canvasMouseY, nx + 145, ny + 19, 14, 12)) {
                    entries.remove(i);
                    saveAndSend();
                    return true;
                }

                nodeIndex++;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xFF111111);

        double canvasMouseX = toCanvasX(mouseX);
        double canvasMouseY = toCanvasY(mouseY);
        ItemStack hoveredResult = ItemStack.EMPTY;

        gfx.pose().pushPose();
        double centerX = this.width / 2.0;
        double centerY = this.height / 2.0;
        gfx.pose().translate(centerX * (1.0 - zoom) + zoomPanX, centerY * (1.0 - zoom) + zoomPanY, 0);
        gfx.pose().scale((float) zoom, (float) zoom, 1.0F);

        // Conexiones entre niveles.
        for (int level = 1; level < 5; level++) {
            int startX = (int) scrollX + (level - 1) * (NODE_WIDTH + COLUMN_SPACING) + NODE_WIDTH;
            int endX = (int) scrollX + level * (NODE_WIDTH + COLUMN_SPACING);
            int startY = (int) scrollY + 10;
            gfx.fill(startX, startY, endX, startY + 2, 0xFF555555);
        }

        for (int level = 1; level <= 5; level++) {
            int levelX = (int) scrollX + (level - 1) * (NODE_WIDTH + COLUMN_SPACING);
            int levelY = (int) scrollY;

            gfx.drawString(this.font, "Nivel " + level, levelX, levelY - 35, 0xFFFFAA00, false);

            boolean hoverAdd = isHovering(canvasMouseX, canvasMouseY, levelX, levelY - 20, NODE_WIDTH, 15);
            gfx.fill(levelX, levelY - 20, levelX + NODE_WIDTH, levelY - 5,
                    hoverAdd ? 0xFF225522 : 0xFF113311);
            gfx.drawCenteredString(this.font, "+ Costo mano secundaria", levelX + NODE_WIDTH / 2,
                    levelY - 16, 0xFF55FF55);

            int nodeIndex = 0;
            for (TradeConfigData.TradeEntry entry : entries) {
                if (entry.requiredLevel != level) continue;

                int nx = levelX;
                int ny = levelY + nodeIndex * (NODE_HEIGHT + NODE_SPACING);
                gfx.fill(nx, ny, nx + NODE_WIDTH, ny + NODE_HEIGHT, 0xFF222222);
                gfx.fill(nx, ny, nx + NODE_WIDTH, ny + 1, 0xFF444444);
                gfx.fill(nx, ny + NODE_HEIGHT - 1, nx + NODE_WIDTH, ny + NODE_HEIGHT, 0xFF111111);
                gfx.fill(nx, ny, nx + 1, ny + NODE_HEIGHT, 0xFF444444);
                gfx.fill(nx + NODE_WIDTH - 1, ny, nx + NODE_WIDTH, ny + NODE_HEIGHT, 0xFF111111);

                ItemStack costStack = new ItemStack(entry.getItem() != null ? entry.getItem() : Items.BARRIER);
                gfx.renderItem(costStack, nx + 4, ny + 10);
                String name = costStack.getHoverName().getString();
                if (name.length() > 8) name = name.substring(0, 7) + ".";
                gfx.drawString(this.font, name, nx + 24, ny + 6, 0xFFFFFFFF, false);
                gfx.drawString(this.font, "Cant: " + entry.minCount + "-" + entry.maxCount,
                        nx + 24, ny + 18, 0xFFAAAAAA, false);

                gfx.drawCenteredString(this.font, "→", nx + 80, ny + 12, 0xFFCCCCCC);
                gfx.fill(nx + 88, ny + 8, nx + 108, ny + 28,
                        isHovering(canvasMouseX, canvasMouseY, nx + 88, ny + 8, 20, 20)
                                ? 0xFF555533 : 0xFF333322);
                ItemStack resultStack = new ItemStack(entry.getResultItem(),
                        Math.max(1, Math.min(entry.resultCount, entry.getResultItem().getMaxStackSize())));
                gfx.renderItem(resultStack, nx + 90, ny + 10);
                gfx.renderItemDecorations(this.font, resultStack, nx + 90, ny + 10);
                if (isHovering(canvasMouseX, canvasMouseY, nx + 88, ny + 8, 20, 20)) {
                    hoveredResult = resultStack;
                }

                gfx.drawString(this.font, entry.weight + "%", nx + 111, ny + 5, 0xFFFFFFAA, false);

                boolean hMinus = isHovering(canvasMouseX, canvasMouseY, nx + 108, ny + 19, 14, 12);
                gfx.fill(nx + 108, ny + 19, nx + 122, ny + 31,
                        hMinus ? 0xFF555555 : 0xFF333333);
                gfx.drawCenteredString(this.font, "-", nx + 115, ny + 21, 0xFFFFFFFF);

                boolean hPlus = isHovering(canvasMouseX, canvasMouseY, nx + 125, ny + 19, 14, 12);
                gfx.fill(nx + 125, ny + 19, nx + 139, ny + 31,
                        hPlus ? 0xFF555555 : 0xFF333333);
                gfx.drawCenteredString(this.font, "+", nx + 132, ny + 21, 0xFFFFFFFF);

                boolean hDel = isHovering(canvasMouseX, canvasMouseY, nx + 145, ny + 19, 14, 12);
                gfx.fill(nx + 145, ny + 19, nx + 159, ny + 31,
                        hDel ? 0xFFAA3333 : 0xFF772222);
                gfx.drawCenteredString(this.font, "x", nx + 152, ny + 21, 0xFFFFFFFF);

                nodeIndex++;
            }
        }
        gfx.pose().popPose();

        gfx.drawString(this.font, "Rueda: zoom (" + Math.round(zoom * 100) + "%) | Arrastra: mover",
                10, 8, 0xFFFFFFFF, true);
        gfx.drawString(this.font, "Mano secundaria: clic en costo o resultado para configurarlo",
                10, 20, 0xFFCCCCCC, true);
        if (!hoveredResult.isEmpty()) {
            gfx.renderTooltip(this.font, hoveredResult, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

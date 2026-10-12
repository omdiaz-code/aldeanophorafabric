package aldeanoforaflut.aldeanoforaflut.entity.client;

import aldeanoforaflut.aldeanoforaflut.ModItems;
import aldeanoforaflut.aldeanoforaflut.PhoraTradeInfo;
import aldeanoforaflut.aldeanoforaflut.TransformableMerchantEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class PhoraMerchantScreen extends AbstractContainerScreen<MerchantMenu> {
    private static final ResourceLocation VILLAGER_LOCATION = new ResourceLocation("textures/gui/container/villager2.png");
    private static final int NUMBER_OF_OFFER_BUTTONS = 7;
    private static final Component DEPRECATED_TOOLTIP = Component.translatable("merchant.deprecated");

    // Cada barra tarda 300 horas de juego en completarse. Al llenarse, el bono sube 10%.
    private static final long TICKS_PER_PERCENT = 3000L;
    private static final long TICKS_PER_CYCLE = TICKS_PER_PERCENT * 100L;

    private int shopItem;
    private final TradeOfferButton[] tradeOfferButtons = new TradeOfferButton[7];
    int scrollOff;
    private boolean isDragging;

    // Access button state
    private String accessModeText = "Todos";
    private boolean isOwner = false;

    // Passive progress bar data
    private long passiveTicks = 0;
    private boolean dataRead = false;
    private int phoraEntityId = -1;
    private PhoraTradeInfo tradeInfo;

    // Fila superior de la zona de tradeos: barra verde pasiva + botón de reinicio
    private static final int TOP_ROW_Y = 2;
    private static final int TOP_ROW_H = 11;
    private static final int PASSIVE_BAR_X = 4;
    private static final int PASSIVE_BAR_W = 80;
    private static final int RESET_BUTTON_X = 88;
    private static final int RESET_BUTTON_SIZE = 11;

    // Botón de reinicio (apaga el Phora y lo vuelve bloque)
    private static final String RESET_SYMBOL = "↻";
    private static final Component RESET_TOOLTIP = Component.translatable("gui.aldeanoforaflut.reset_tooltip");
    private static final Component RESET_KEY_REQUIRED_TOOLTIP = Component.translatable("gui.aldeanoforaflut.reset_requires_key");
    private static final Component RESET_CONFIRM_TOOLTIP = Component.literal("Haz clic otra vez para confirmar. Se pierden el nivel y los tradeos.").withStyle(ChatFormatting.RED);
    private static final long RESET_CONFIRM_MS = 3000L;
    private Button resetButton;
    private long resetArmedUntil = 0;

    public PhoraMerchantScreen(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 276;
        this.inventoryLabelX = 107;
    }

    // Allow setting access info from outside (e.g. ModClientForgeEvents)
    public void setAccessInfo(String modeText, boolean owner) {
        this.accessModeText = modeText;
        this.isOwner = owner;
    }

    /** Lee los datos que el servidor envía en PhoraTradeInfo al abrir el menú */
    private void readPhoraData() {
        if (dataRead) return;
        PhoraTradeInfo info = ClientPacketHandler.getTradeInfo(this.menu.containerId);
        if (info != null) {
            this.tradeInfo = info;
            this.passiveTicks = info.passiveTicks();
            this.isOwner = info.isOwner();
            this.phoraEntityId = info.entityId();
            this.accessModeText = switch (info.accessMode()) {
                case 0 -> "Solo yo";
                case 1 -> "Mi Hermandad";
                default -> "Todos";
            };
            this.dataRead = true;
        }
    }

    private int getRequiredLevel(int offerIndex) {
        return this.tradeInfo != null ? this.tradeInfo.getOfferLevel(offerIndex) : 0;
    }

    private int getPassivePercent() {
        return (int) Math.min(100L, (this.passiveTicks / TICKS_PER_CYCLE) * 10L);
    }

    private float getPassiveCycleProgress() {
        if (this.getPassivePercent() >= 100) return 1.0F;
        return (float) (this.passiveTicks % TICKS_PER_CYCLE) / (float) TICKS_PER_CYCLE;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // El servidor conserva el contador real; esto mantiene la barra animada
        // entre las actualizaciones del menú mientras el Phora está abierto.
        if (this.dataRead && this.passiveTicks < Long.MAX_VALUE) {
            this.passiveTicks++;
        }
    }

    private void postButtonClick() {
        this.menu.setSelectionHint(this.shopItem);
        this.menu.tryMoveItems(this.shopItem);
        this.minecraft.getConnection().send(new ServerboundSelectTradePacket(this.shopItem));
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        int k = j + 16 + 2;

        for (int l = 0; l < 7; ++l) {
            this.tradeOfferButtons[l] = this.addRenderableWidget(new TradeOfferButton(i + 5, k, l, (btn) -> {
                if (btn instanceof TradeOfferButton tradeBtn) {
                    this.shopItem = tradeBtn.getIndex() + this.scrollOff;
                    this.postButtonClick();
                }
            }));
            k += 20;
        }

        this.resetButton = this.addRenderableWidget(new PhoraResetButton(
                i + RESET_BUTTON_X, j + TOP_ROW_Y, btn -> this.onResetClicked()));
        this.resetButton.setTooltip(Tooltip.create(RESET_TOOLTIP));
        this.resetButton.visible = false;
    }

    private boolean isResetArmed() {
        return System.currentTimeMillis() < this.resetArmedUntil;
    }

    /** Primer clic arma el botón; el segundo (antes de RESET_CONFIRM_MS) envía el reinicio. */
    private void onResetClicked() {
        if (this.phoraEntityId == -1) return;
        if (this.isResetArmed()) {
            ClientNetworking.sendReset(this.phoraEntityId);
            this.onClose();
        } else {
            this.resetArmedUntil = System.currentTimeMillis() + RESET_CONFIRM_MS;
            this.resetButton.setTooltip(Tooltip.create(RESET_CONFIRM_TOOLTIP));
        }
    }

    private void updateResetButton() {
        boolean canReset = this.isOwner || (this.minecraft.player != null && this.minecraft.player.hasPermissions(2));
        boolean hasKey = ModItems.hasCopperKey(this.minecraft.player);
        this.resetButton.visible = canReset && this.phoraEntityId != -1;
        this.resetButton.active = hasKey;
        this.resetButton.setTooltip(Tooltip.create(hasKey ? RESET_TOOLTIP : RESET_KEY_REQUIRED_TOOLTIP));
        if (this.resetArmedUntil != 0 && !this.isResetArmed()) {
            this.resetArmedUntil = 0;
            this.resetButton.setTooltip(Tooltip.create(RESET_TOOLTIP));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Título centrado, como en la interfaz de referencia.
        int titleX = (this.imageWidth - this.font.width(this.title)) / 2;
        gfx.drawString(this.font, this.title, titleX, -12, 0xFFE1C18A, false);

        // 2. "Nivel X" text above progress bar
        int level = this.menu.getTraderLevel();
        if (level > 0 && level <= 5 && this.menu.showProgressBar()) {
            Component nivelComp = Component.literal("Nivel " + level);
            int nivelW = this.font.width(nivelComp);
            int nivelX = 136 + 51 - nivelW / 2;
            gfx.drawString(this.font, nivelComp, nivelX, 6, 0xFFDA6A, false);
        }

        // 3. "Inventario" label
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xAAAAAA, false);

        // 4. Access button text (only for owner)
        if (this.isOwner) {
            Component accessComp = Component.literal("Acceso: " + this.accessModeText + " >");
            int accessW = this.font.width(accessComp);
            // Position below the trade slots area
            gfx.drawString(this.font, accessComp, 136 + 51 - accessW / 2, 60, 0xFF8C00, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Marco metálico oscuro con esquinas de cobre, inspirado en la referencia.
        drawIndustrialFrame(gfx, i - 2, j - 16, this.imageWidth + 4, this.imageHeight + 18);

        // Redraw trade list panel area (left side background)
        gfx.fill(i + 4, j + 15, i + 99, j + 158, 0xFF211C1A);
        drawBorder(gfx, i + 4, j + 15, 95, 143, 0xFF65503A);
        gfx.fill(i + 5, j + 16, i + 7, j + 157, 0xFF74351D);

        // Barra pasiva: un ciclo completo incrementa 10% el indicador de la derecha.
        {
            int barX = i + PASSIVE_BAR_X;
            int barY = j + TOP_ROW_Y;
            int barW = PASSIVE_BAR_W;
            int barH = TOP_ROW_H;
            gfx.fill(barX, barY, barX + barW, barY + barH, 0xFF171B17);
            int fillW = Mth.clamp(Mth.floor(this.getPassiveCycleProgress() * (barW - 2)), 0, barW - 2);
            if (fillW > 0) {
                gfx.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, 0xFF27A94B);
                gfx.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 3, 0xFF61E779);
            }
            drawBorder(gfx, barX, barY, barW, barH, 0xFFD47A31);
        }

        // Redraw right panel (trade + inventory area)
        gfx.fill(i + 100, j + 2, i + 274, j + 164, 0xFF17191A);
        drawBorder(gfx, i + 100, j + 2, 174, 162, 0xFF343638);
        gfx.fill(i + 102, j + 3, i + 272, j + 4, 0xFF80603A);

        // Dibujar el indicador después del fondo del panel para que no quede tapado.
        int percentX = i + 247;
        int percentY = j + TOP_ROW_Y - 1;
        gfx.fill(percentX, percentY, percentX + 26, percentY + 13, 0xFF17191A);
        drawBorder(gfx, percentX, percentY, 26, 13, 0xFFD47A31);
        String passiveBonus = this.getPassivePercent() + "%";
        int percentTextX = percentX + (26 - this.font.width(passiveBonus)) / 2;
        gfx.drawString(this.font, passiveBonus, percentTextX, percentY + 3, 0xFFE9C67F, false);

        // Trade input/output slots background
        // Slot 1 (input 1)
        drawSlot(gfx, i + 136, j + 37);
        // Slot 2 (input 2)
        drawSlot(gfx, i + 162, j + 37);
        // Arrow between inputs and result
        gfx.drawString(this.font, "->", i + 185, j + 42, 0xAAAAAA, false);
        // Result slot (bigger)
        drawSlot(gfx, i + 220, j + 37);

        // Player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = i + 108 + col * 18;
                int slotY = j + 84 + row * 18;
                drawSlot(gfx, slotX, slotY);
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            int slotX = i + 108 + col * 18;
            int slotY = j + 142;
            drawSlot(gfx, slotX, slotY);
        }

        // Barra de nivel con el mismo XP acumulado que usa el aldeano.
        int level = this.menu.getTraderLevel();
        if (level > 0 && level <= 5 && this.menu.showProgressBar()) {
            int barX = i + 136;
            int barY = j + 16;
            int barW = 102;
            int barH = 7;
            gfx.fill(barX, barY, barX + barW, barY + barH, 0xFF2A2925);
            int xp = this.menu.getTraderXp();
            int minXp = TransformableMerchantEntity.getXpForLevel(level);
            int maxXp = level >= 5 ? minXp : TransformableMerchantEntity.getXpForLevel(level + 1);
            float ratio = level >= 5 ? 1.0F : maxXp > minXp
                    ? Mth.clamp((float) (xp - minXp) / (float) (maxXp - minXp), 0.0F, 1.0F)
                    : 0.0F;
            int fillW = Mth.floor(ratio * (barW - 2));
            if (fillW > 0) {
                gfx.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, 0xFFD47A22);
                gfx.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 3, 0xFFFFB84B);
            }
            drawBorder(gfx, barX, barY, barW, barH, 0xFFD47A31);
        }

        // Cross-out red if selected trade is out of stock
        MerchantOffers merchantoffers = this.menu.getOffers();
        if (!merchantoffers.isEmpty()) {
            int k = this.shopItem;
            if (k >= 0 && k < merchantoffers.size()) {
                MerchantOffer merchantoffer = merchantoffers.get(k);
                if (merchantoffer.isOutOfStock()) {
                    gfx.blit(VILLAGER_LOCATION, this.leftPos + 83 + 99, this.topPos + 35, 0, 311.0F, 0.0F, 28, 21, 512, 256);
                }
            }
        }
    }

    private void drawSlot(GuiGraphics gfx, int x, int y) {
        gfx.fill(x - 1, y - 1, x + 17, y + 17, 0xFF555555);
        gfx.fill(x, y, x + 16, y + 16, 0xFF1A1A1A);
    }

    private void drawIndustrialFrame(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0xFF090B0C);
        gfx.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF28292A);
        gfx.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFF111314);
        drawBorder(gfx, x, y, w, h, 0xFF4C453B);
        drawFrameCorner(gfx, x, y, w, h, true, true);
        drawFrameCorner(gfx, x, y, w, h, false, true);
        drawFrameCorner(gfx, x, y, w, h, true, false);
        drawFrameCorner(gfx, x, y, w, h, false, false);
    }

    private void drawFrameCorner(GuiGraphics gfx, int x, int y, int w, int h, boolean left, boolean top) {
        int cornerX = left ? x : x + w - 2;
        int lineX = left ? x : x + w - 10;
        int cornerY = top ? y : y + h - 2;
        int lineY = top ? y : y + h - 8;
        gfx.fill(lineX, cornerY, lineX + 10, cornerY + 2, 0xFF8A431D);
        gfx.fill(lineX, cornerY, lineX + 8, cornerY + 1, 0xFFE7A047);
        gfx.fill(cornerX, lineY, cornerX + 2, lineY + 8, 0xFF8A431D);
        gfx.fill(cornerX, lineY, cornerX + 1, lineY + 6, 0xFFE7A047);
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x, y, x + w, y + 1, color);       // top
        gfx.fill(x, y + h - 1, x + w, y + h, color); // bottom
        gfx.fill(x, y, x + 1, y + h, color);         // left
        gfx.fill(x + w - 1, y, x + w, y + h, color); // right
    }

    private void renderScroller(GuiGraphics gfx, int guiLeft, int guiTop, MerchantOffers offers) {
        int i = offers.size() + 1 - 7;
        if (i > 1) {
            int j = 139 - (27 + (i - 1) * 139 / i);
            int k = 1 + j / i + 139 / i;
            int i1 = Math.min(113, this.scrollOff * k);
            if (this.scrollOff == i - 1) {
                i1 = 113;
            }
            // Orange scrollbar
            gfx.fill(guiLeft + 94, guiTop + 18 + i1, guiLeft + 100, guiTop + 18 + i1 + 27, 0xFFFF8C00);
        } else {
            // No scroll needed, draw inactive scroller
            gfx.fill(guiLeft + 94, guiTop + 18, guiLeft + 100, guiTop + 18 + 27, 0xFF555555);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.readPhoraData();
        this.updateResetButton();
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        MerchantOffers merchantoffers = this.menu.getOffers();
        if (!merchantoffers.isEmpty()) {
            int i = (this.width - this.imageWidth) / 2;
            int j = (this.height - this.imageHeight) / 2;
            int k = j + 16 + 1;
            int l = i + 5 + 5;
            this.renderScroller(gfx, i, j, merchantoffers);
            int i1 = 0;

            for (MerchantOffer merchantoffer : merchantoffers) {
                if (!this.canScroll(merchantoffers.size()) || i1 >= this.scrollOff && i1 < 7 + this.scrollOff) {
                    ItemStack itemstack = merchantoffer.getBaseCostA();
                    ItemStack itemstack1 = merchantoffer.getCostA();
                    ItemStack itemstack2 = merchantoffer.getCostB();
                    ItemStack itemstack3 = merchantoffer.getResult();

                    gfx.pose().pushPose();
                    gfx.pose().translate(0.0F, 0.0F, 100.0F);
                    int j1 = k + 2;

                    // Check if locked
                    int reqLvl = this.getRequiredLevel(i1);
                    boolean isLocked = reqLvl > 0 && merchantoffer.isOutOfStock();

                    this.renderAndDecorateCostA(gfx, itemstack1, itemstack, l, j1);
                    if (!itemstack2.isEmpty()) {
                        gfx.renderFakeItem(itemstack2, i + 5 + 35, j1);
                        gfx.renderItemDecorations(this.font, itemstack2, i + 5 + 35, j1);
                    }

                    // Arrow or "Nv> X" text
                    if (isLocked) {
                        // Alineado a la derecha para terminar justo antes del ítem resultado (x = i + 5 + 68)
                        String nvText = "Nv> " + reqLvl;
                        int textWidth = this.font.width(nvText);
                        gfx.drawString(this.font, nvText, i + 5 + 66 - textWidth, j1 + 4, 0xFFFFFF, true);
                    } else {
                        this.renderButtonArrows(gfx, merchantoffer, i, j1);
                    }

                    gfx.renderFakeItem(itemstack3, i + 5 + 68, j1);
                    gfx.renderItemDecorations(this.font, itemstack3, i + 5 + 68, j1);
                    gfx.pose().popPose();
                    k += 20;
                    ++i1;
                } else {
                    ++i1;
                }
            }

            int k1 = this.shopItem;
            if (k1 >= 0 && k1 < merchantoffers.size()) {
                MerchantOffer merchantoffer1 = merchantoffers.get(k1);
                if (this.menu.showProgressBar()) {
                    this.renderProgressBar(gfx, i, j, merchantoffer1);
                }

                if (merchantoffer1.isOutOfStock() && this.isHovering(186, 35, 22, 21, (double) mouseX, (double) mouseY) && this.menu.canRestock()) {
                    gfx.renderTooltip(this.font, DEPRECATED_TOOLTIP, mouseX, mouseY);
                }
            }

            for (TradeOfferButton btn : this.tradeOfferButtons) {
                if (btn.isHoveredOrFocused()) {
                    btn.renderToolTip(gfx, mouseX, mouseY);
                }
                btn.visible = btn.index < this.menu.getOffers().size();
            }

            RenderSystem.enableDepthTest();
        }

        this.renderTooltip(gfx, mouseX, mouseY);
    }

    private void renderButtonArrows(GuiGraphics gfx, MerchantOffer offer, int guiLeft, int y) {
        RenderSystem.enableBlend();
        if (offer.isOutOfStock()) {
            gfx.blit(VILLAGER_LOCATION, guiLeft + 5 + 35 + 20, y + 3, 0, 25.0F, 171.0F, 10, 9, 512, 256);
        } else {
            gfx.blit(VILLAGER_LOCATION, guiLeft + 5 + 35 + 20, y + 3, 0, 15.0F, 171.0F, 10, 9, 512, 256);
        }
    }

    private void renderAndDecorateCostA(GuiGraphics gfx, ItemStack costA, ItemStack baseCostA, int x, int y) {
        gfx.renderFakeItem(costA, x, y);
        if (baseCostA.getCount() == costA.getCount()) {
            gfx.renderItemDecorations(this.font, costA, x, y);
        } else {
            gfx.renderItemDecorations(this.font, baseCostA, x, y, baseCostA.getCount() == 1 ? "1" : null);
            gfx.pose().pushPose();
            gfx.pose().translate(0.0F, 0.0F, 200.0F);
            String count = costA.getCount() == 1 ? "1" : String.valueOf(costA.getCount());
            font.drawInBatch(count, (float) (x + 14) + 19 - 2 - font.width(count), (float) y + 6 + 3, 0xFFFFFF, true, gfx.pose().last().pose(), gfx.bufferSource(), net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880, false);
            gfx.pose().popPose();
            gfx.pose().pushPose();
            gfx.pose().translate(0.0F, 0.0F, 300.0F);
            gfx.blit(VILLAGER_LOCATION, x + 7, y + 12, 0, 0.0F, 176.0F, 9, 2, 512, 256);
            gfx.pose().popPose();
        }
    }

    private void renderProgressBar(GuiGraphics gfx, int guiLeft, int guiTop, MerchantOffer offer) {
        // We draw our own orange bar in renderBg, so this is a no-op
    }

    private boolean canScroll(int offerCount) {
        return offerCount > 7;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int i = this.menu.getOffers().size();
        if (this.canScroll(i)) {
            int j = i - 7;
            this.scrollOff = Mth.clamp((int) ((double) this.scrollOff - delta), 0, j);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int i = this.menu.getOffers().size();
        if (this.isDragging) {
            int j = this.topPos + 18;
            int k = j + 139;
            int l = i - 7;
            float f = ((float) mouseY - (float) j - 13.5F) / ((float) (k - j) - 27.0F);
            f = f * (float) l + 0.5F;
            this.scrollOff = Mth.clamp((int) f, 0, l);
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.isDragging = false;
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (this.canScroll(this.menu.getOffers().size()) && mouseX > (double) (i + 94) && mouseX < (double) (i + 94 + 6) && mouseY > (double) (j + 18) && mouseY <= (double) (j + 18 + 139 + 1)) {
            this.isDragging = true;
        }

        // Check if "Acceso" text area was clicked (only for owner)
        if (this.isOwner) {
            int accessX = i + 136 + 51 - 40;
            int accessY = j + 60;
            if (mouseX >= accessX && mouseX <= accessX + 80 && mouseY >= accessY && mouseY <= accessY + 12) {
                if (this.phoraEntityId != -1) {
                    ClientNetworking.sendCycleAccess(this.phoraEntityId);
                }
                
                // Cycle locally immediately for responsiveness
                if (this.accessModeText.equals("Todos")) {
                    this.accessModeText = "Solo yo";
                } else if (this.accessModeText.equals("Solo yo")) {
                    this.accessModeText = "Mi Hermandad";
                } else {
                    this.accessModeText = "Todos";
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Botón cuadrado pequeño con el símbolo de reinicio; se pone rojo mientras espera confirmación. */
        class PhoraResetButton extends Button {

        PhoraResetButton(int x, int y, Button.OnPress onPress) {
            super(x, y, RESET_BUTTON_SIZE, RESET_BUTTON_SIZE, Component.literal(RESET_SYMBOL), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            boolean armed = PhoraMerchantScreen.this.isResetArmed();
            int border = armed ? 0xFFFF3333 : !this.active ? 0xFF444444 : this.isHoveredOrFocused() ? 0xFFFF8C00 : 0xFF555555;
            int symbolColor = armed ? 0xFFFF5555 : !this.active ? 0xFF777777 : 0xFFDDDDDD;

            int x = this.getX();
            int y = this.getY();
            gfx.fill(x, y, x + this.width, y + this.height, 0xFF1A1A1A);
            drawBorder(gfx, x, y, this.width, this.height, border);

            int symbolW = PhoraMerchantScreen.this.font.width(RESET_SYMBOL);
            gfx.drawString(PhoraMerchantScreen.this.font, RESET_SYMBOL,
                    x + (this.width - symbolW) / 2 + 1, y + 2, symbolColor, false);
        }
    }

        class TradeOfferButton extends Button {
        final int index;

        public TradeOfferButton(int x, int y, int index, Button.OnPress onPress) {
            super(x, y, 88, 20, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.index = index;
            this.visible = false;
        }

        public int getIndex() {
            return this.index;
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int offerIndex = this.index + PhoraMerchantScreen.this.scrollOff;
            MerchantOffers offers = PhoraMerchantScreen.this.menu.getOffers();
            boolean locked = offerIndex < offers.size()
                    && PhoraMerchantScreen.this.getRequiredLevel(offerIndex) > 0
                    && offers.get(offerIndex).isOutOfStock();
            boolean selected = offerIndex == PhoraMerchantScreen.this.shopItem;
            boolean highlighted = selected || this.isHoveredOrFocused();
            int background = highlighted ? 0xFFB77C38 : locked ? 0xFF79130F : 0xFF454544;
            int edge = highlighted ? 0xFFFFBD59 : locked ? 0xFFA22A1E : 0xFF66605A;

            gfx.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, background);
            gfx.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + 3,
                    highlighted ? 0xFFE8B85E : locked ? 0xFF9B251B : 0xFF5B5B59);
            gfx.fill(this.getX() + 1, this.getY() + this.height - 2, this.getX() + this.width - 1,
                    this.getY() + this.height - 1, 0xFF292522);
            PhoraMerchantScreen.this.drawBorder(gfx, this.getX(), this.getY(), this.width, this.height, edge);
        }

        public void renderToolTip(GuiGraphics gfx, int mouseX, int mouseY) {
            if (this.isHovered && PhoraMerchantScreen.this.menu.getOffers().size() > this.index + PhoraMerchantScreen.this.scrollOff) {
                if (mouseX < this.getX() + 20) {
                    ItemStack itemstack = PhoraMerchantScreen.this.menu.getOffers().get(this.index + PhoraMerchantScreen.this.scrollOff).getCostA();
                    gfx.renderTooltip(PhoraMerchantScreen.this.font, itemstack, mouseX, mouseY);
                } else if (mouseX < this.getX() + 50 && mouseX > this.getX() + 30) {
                    ItemStack itemstack2 = PhoraMerchantScreen.this.menu.getOffers().get(this.index + PhoraMerchantScreen.this.scrollOff).getCostB();
                    if (!itemstack2.isEmpty()) {
                        gfx.renderTooltip(PhoraMerchantScreen.this.font, itemstack2, mouseX, mouseY);
                    }
                } else if (mouseX > this.getX() + 65) {
                    ItemStack itemstack1 = PhoraMerchantScreen.this.menu.getOffers().get(this.index + PhoraMerchantScreen.this.scrollOff).getResult();
                    gfx.renderTooltip(PhoraMerchantScreen.this.font, itemstack1, mouseX, mouseY);
                }
            }
        }
    }
}

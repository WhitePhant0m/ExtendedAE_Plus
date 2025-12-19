package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import com.extendedae_plus.helper.IUpgradeableMenuCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * PatternProviderScreen的兼容性Mixin
 * 优先级设置为500，避免与appflux冲突
 */
@Mixin(value = PatternProviderScreen.class, priority = 500, remap = false)
public abstract class PatternProviderScreenCompatMixin<C extends PatternProviderMenu> extends AEBaseScreen<C> {
    public PatternProviderScreenCompatMixin(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void eap$initCompatUpgrades(PatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        if (UpgradeSlotCompat.shouldAddUpgradePanelToScreen()) {
            this.widgets.add("upgrades", new UpgradesPanel(
                    menu.getSlots(SlotSemantics.UPGRADE),
                    this::eap$getCompatibleUpgrades
            ));
        }
    }

    @Unique
    private List<Component> eap$getCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());
        list.addAll(Upgrades.getTooltipLinesForMachine(((IUpgradeableMenuCompat) menu).eap$getCompatUpgrades().getUpgradableItem()));
        return list;
    }
}

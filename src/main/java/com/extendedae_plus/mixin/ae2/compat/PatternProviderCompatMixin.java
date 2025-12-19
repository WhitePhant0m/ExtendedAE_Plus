package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import com.extendedae_plus.helper.IUpgradeableMenuCompat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PatternProviderMenu的兼容性Mixin
 * 优先级设置为500，低于appflux的默认优先级，避免冲突
 */
@Mixin(value = PatternProviderMenu.class, priority = 500, remap = false)
public abstract class PatternProviderCompatMixin extends AEBaseMenu implements IUpgradeableMenuCompat {
    @Final
    @Shadow(remap = false)
    protected PatternProviderLogic logic;

    public PatternProviderCompatMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/patternprovider/PatternProviderLogicHost;)V",
            at = @At("TAIL"),
            remap = false)
    private void eap$initCompatUpgrades(MenuType<?> menuType, int id, Inventory playerInventory, PatternProviderLogicHost host, CallbackInfo ci) {
        if (UpgradeSlotCompat.shouldEnableUpgradeSlots()) {
            this.setupUpgrades(((IUpgradeableObject) host).getUpgrades());
        }
    }

    @Override
    public IUpgradeInventory eap$getCompatUpgrades() {
        return ((IUpgradeableObject) this.logic).getUpgrades();
    }
}

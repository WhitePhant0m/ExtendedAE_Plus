package com.extendedae_plus.mixin.advancedae.compat;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.menu.AEBaseMenu;
import com.extendedae_plus.helper.IUpgradeableMenuCompat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import net.pedroksl.advanced_ae.gui.advpatternprovider.AdvPatternProviderMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AdvPatternProviderMenu.class, remap = false)
public class AdvPatternProviderMenuMixin extends AEBaseMenu implements IUpgradeableMenuCompat {
    @Unique
    private IUpgradeableObject eap$host;

    public AdvPatternProviderMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogicHost;)V",at = @At("TAIL"))
    private void eap$initCompatUpgrades(MenuType menuType, int id, Inventory playerInventory, AdvPatternProviderLogicHost host, CallbackInfo ci) {
        this.eap$host = (IUpgradeableObject) host;
        eap$setupUpgrades();
    }

    @Unique
    public void eap$setupUpgrades() {
        setupUpgrades(this.eap$host.getUpgrades());
    }

    @Unique
    public IUpgradeableObject eap$getHost() {
        return this.eap$host;
    }

    @Override
    public IUpgradeInventory eap$getCompatUpgrades() {
        return eap$getHost().getUpgrades();
    }
}

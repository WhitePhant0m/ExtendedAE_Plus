package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * PatternProviderLogicHost的兼容性Mixin
 * 优先级设置为500，避免与appflux冲突
 */
@Mixin(value = PatternProviderLogicHost.class, priority = 500, remap = false)
public interface PatternProviderLogicHostCompatMixin extends IUpgradeableObject {
    @Shadow
    PatternProviderLogic getLogic();

    @Override
    default IUpgradeInventory getUpgrades() {
        return ((IUpgradeableObject) this.getLogic()).getUpgrades();
    }
}

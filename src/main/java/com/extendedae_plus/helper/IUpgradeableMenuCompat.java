package com.extendedae_plus.helper;

import appeng.api.upgrades.IUpgradeInventory;
import org.spongepowered.asm.mixin.Unique;

public interface IUpgradeableMenuCompat {
    @Unique
    IUpgradeInventory eap$getCompatUpgrades();
}

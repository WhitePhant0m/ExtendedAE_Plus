package com.extendedae_plus.mixin.appflux;

import appeng.api.upgrades.IUpgradeableObject;
import com.bawnorton.mixinsquared.TargetHandler;
import com.extendedae_plus.api.bridge.IInterfaceWirelessLinkBridge;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 当appflux存在时，修改PatternProviderLogic的升级槽数量为2个
 * 优先级设置为2000，确保在appflux之后应用
 */
@Mixin(value = AdvPatternProviderLogic.class, priority = 2000, remap = false)
public abstract class AdvUpgradeSlotMixin implements IUpgradeableObject {
    @TargetHandler(
            mixin = "net.pedroksl.advanced_ae.mixins.appflux.MixinAdvPatternProviderLogic",
            name = "initUpgrade")
    @ModifyArg(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/upgrades/UpgradeInventories;forMachine(Lnet/minecraft/world/level/ItemLike;ILappeng/api/upgrades/MachineUpgradesChanged;)Lappeng/api/upgrades/IUpgradeInventory;"))
    private int modifyMaxUpgrades(int maxUpgrades) {
        return UpgradeSlotCompat.shouldEnableUpgradeSlots() ? maxUpgrades + 1 : maxUpgrades;
    }

    @TargetHandler(
            mixin = "net.pedroksl.advanced_ae.mixins.appflux.MixinAdvPatternProviderLogic",
            name = "af_$onUpgradesChanged")
    @Inject(method = "@MixinSquared:Handler", at = @At("TAIL"), remap = false, require = 0)
    private void eap$onAppfluxUpgradesChanged(CallbackInfo ci) {
        if ((Object) this instanceof IInterfaceWirelessLinkBridge bridge) {
            bridge.eap$syncVirtualCraftingState();
            if (UpgradeSlotCompat.shouldEnableChannelCard()) {
                bridge.eap$resetChannel();
                bridge.eap$initializeChannelLink();
            }
        }
    }
}

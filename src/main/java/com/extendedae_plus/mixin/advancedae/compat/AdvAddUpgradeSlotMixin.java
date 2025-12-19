package com.extendedae_plus.mixin.advancedae.compat;

import appeng.api.networking.IManagedGridNode;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import com.extendedae_plus.api.bridge.IInterfaceWirelessLinkBridge;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = AdvPatternProviderLogic.class, priority = 2000, remap = false)
public class AdvAddUpgradeSlotMixin implements IUpgradeableObject {
    @Shadow
    @Final
    private AdvPatternProviderLogicHost host;

    @Unique
    private IUpgradeInventory eap$compatUpgrades;

    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogicHost;I)V",
            at = @At("TAIL"))
    private void eap$compatInitUpgrades(IManagedGridNode mainNode, AdvPatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        if (UpgradeSlotCompat.shouldEnableUpgradeSlots())
            eap$compatUpgrades = UpgradeInventories.forMachine(host.getTerminalIcon().getItem(), 2, this::eap$compatOnUpgradesChanged);
    }

    @Unique
    private void eap$compatOnUpgradesChanged() {
        this.host.saveChanges();
        if ((Object) this instanceof IInterfaceWirelessLinkBridge bridge) {
            bridge.eap$syncVirtualCraftingState();
            if (UpgradeSlotCompat.shouldEnableChannelCard()) {
                bridge.eap$resetChannel();
                bridge.eap$initializeChannelLink();
            }
        }
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void eap$compatSaveUpgrades(CompoundTag tag, CallbackInfo ci) {
        if (UpgradeSlotCompat.shouldEnableUpgradeSlots() && this.eap$compatUpgrades != null) {
            this.eap$compatUpgrades.writeToNBT(tag, "upgrades");
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void eap$compatLoadUpgrades(CompoundTag tag, CallbackInfo ci) {
        if (UpgradeSlotCompat.shouldEnableUpgradeSlots() && this.eap$compatUpgrades != null) {
            this.eap$compatUpgrades.readFromNBT(tag, "upgrades");
            if ((Object) this instanceof IInterfaceWirelessLinkBridge bridge) {
                if (UpgradeSlotCompat.shouldEnableChannelCard()) {
                    bridge.eap$resetChannel();
                    bridge.eap$initializeChannelLink();
                }
                bridge.eap$syncVirtualCraftingState();
            }
        }
    }

    @Inject(method = "addDrops", at = @At("TAIL"))
    private void eap$compatDropUpgrades(List<ItemStack> drops, CallbackInfo ci) {
        if (UpgradeSlotCompat.shouldEnableUpgradeSlots() && this.eap$compatUpgrades != null) {
            for (var stack : this.eap$compatUpgrades) {
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void eap$compatClearUpgrades(CallbackInfo ci) {
        if (UpgradeSlotCompat.shouldEnableUpgradeSlots() && this.eap$compatUpgrades != null) {
            this.eap$compatUpgrades.clear();
            if ((Object) this instanceof IInterfaceWirelessLinkBridge bridge) {
                bridge.eap$syncVirtualCraftingState();
            }
        }
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return eap$compatUpgrades;
    }
}

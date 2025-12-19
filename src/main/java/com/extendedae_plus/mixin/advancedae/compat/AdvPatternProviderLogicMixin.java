package com.extendedae_plus.mixin.advancedae.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeableObject;
import com.extendedae_plus.api.bridge.IInterfaceWirelessLinkBridge;
import com.extendedae_plus.compat.PatternProviderLogicVirtualCompatBridge;
import com.extendedae_plus.helper.WirelessLogicHelper;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AdvPatternProviderLogic.class, priority = 500, remap = false)
public abstract class AdvPatternProviderLogicMixin implements IUpgradeableObject, IInterfaceWirelessLinkBridge, PatternProviderLogicVirtualCompatBridge {

    @Unique
    private WirelessLogicHelper eap$wirelessHelper;

    @Final
    @Shadow
    private AdvPatternProviderLogicHost host;

    @Final
    @Shadow
    private IManagedGridNode mainNode;

    @Shadow
    public abstract IGrid getGrid();

    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogicHost;I)V", at = @At("TAIL"))
    private void eap$initWirelessHelper(IManagedGridNode mainNode, AdvPatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        eap$wirelessHelper = new WirelessLogicHelper(host::getBlockEntity, mainNode, this::getGrid, this::getUpgrades);
    }

    @Override
    public void eap$syncVirtualCraftingState() {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.syncVirtualCraftingState();
        }
    }

    @Override
    public void eap$resetChannel() {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.resetChannel();
        }
    }

    @Override
    public boolean eap$compatIsVirtualCraftingEnabled() {
        return eap$wirelessHelper != null && eap$wirelessHelper.isVirtualCraftingEnabled();
    }

    @Override
    public IGrid eap$compatGetGrid() {
        return this.getGrid();
    }

    @Override
    public IManagedGridNode eap$compatGetMainNode() {
        return this.mainNode;
    }

    @Inject(method = "pushPattern", at = @At("HEAD"))
    private void eap$compatOnPushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.tryVirtualCompletion(patternDetails);
        }
    }

    @Override
    public void eap$updateWirelessLink() {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.updateWirelessLink();
        }
    }

    @Override
    public void eap$initializeChannelLink() {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.initializeChannelLink();
        }
    }

    @Override
    public void eap$setClientWirelessState(boolean connected) {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.setClientWirelessState(connected);
        }
    }

    @Override
    public boolean eap$isWirelessConnected() {
        return eap$wirelessHelper != null && eap$wirelessHelper.isWirelessConnected();
    }

    @Override
    public boolean eap$hasTickInitialized() {
        return eap$wirelessHelper == null || eap$wirelessHelper.hasTickInitialized();
    }

    @Override
    public void eap$setTickInitialized(boolean initialized) {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.setTickInitialized(initialized);
        }
    }

    @Override
    public void eap$handleDelayedInit() {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.handleDelayedInit();
        }
    }

    @Inject(method = "onMainNodeStateChanged", at = @At("TAIL"))
    private void eap$compatOnMainNodeStateChangedTail(CallbackInfo ci) {
        if (eap$wirelessHelper != null) {
            eap$wirelessHelper.onMainNodeStateChanged();
        }
    }
}

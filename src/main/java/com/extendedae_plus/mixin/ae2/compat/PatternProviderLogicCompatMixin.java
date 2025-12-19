package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.extendedae_plus.ae.wireless.WirelessSlaveLink;
import com.extendedae_plus.ae.wireless.endpoint.GenericNodeEndpointImpl;
import com.extendedae_plus.api.bridge.IInterfaceWirelessLinkBridge;
import com.extendedae_plus.compat.PatternProviderLogicVirtualCompatBridge;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import com.extendedae_plus.init.ModItems;
import com.extendedae_plus.items.materials.ChannelCardItem;
import com.extendedae_plus.mixin.ae2.accessor.CraftingCpuLogicAccessor;
import com.extendedae_plus.mixin.ae2.accessor.ExecutingCraftingJobAccessor;
import com.extendedae_plus.util.Logger;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PatternProviderLogic的兼容性Mixin
 * 优先级设置为1500，在appflux之后应用
 * 根据appflux是否存在来决定是否实现IUpgradeableObject接口
 */
@Mixin(value = PatternProviderLogic.class, priority = 500, remap = false)
public abstract class PatternProviderLogicCompatMixin implements IUpgradeableObject, IInterfaceWirelessLinkBridge, PatternProviderLogicVirtualCompatBridge {

    @Unique
    private WirelessSlaveLink eap$compatLink;

    @Unique
    private long eap$compatLastChannel = -1;

    @Unique
    private boolean eap$compatClientConnected = false;

    @Unique
    private boolean eap$compatHasInitialized = false;

    @Unique
    private int eap$compatDelayedInitTicks = 0;

    @Final
    @Shadow
    private PatternProviderLogicHost host;

    @Final
    @Shadow
    private IManagedGridNode mainNode;

    @Final
    @Shadow
    private IActionSource actionSource;

    @Unique
    private boolean eap$compatVirtualCraftingEnabled = false;

    @Shadow
    public abstract IGrid getGrid();

    @Override
    public void eap$syncVirtualCraftingState() {
        boolean hasCard = false;
        var inventory = getUpgrades();
        if (inventory != null) {
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty() && stack.getItem() == ModItems.VIRTUAL_CRAFTING_CARD.get()) {
                    hasCard = true;
                    break;
                }
            }
        }
        eap$compatVirtualCraftingEnabled = hasCard;
    }

    @Override
    public void eap$resetChannel() {
        eap$compatLastChannel = -1;
        eap$compatHasInitialized = false;
    }

    @Unique
    private void eap$compatTryVirtualCompletion(IPatternDetails patternDetails) {
        if (!eap$compatVirtualCraftingEnabled) {
            return;
        }

        var be = this.host.getBlockEntity();
        if (be == null || be.getLevel() == null || be.getLevel().isClientSide) {
            return;
        }

        var grid = getGrid();
        if (grid == null) {
            return;
        }

        var craftingService = grid.getCraftingService();
        if (craftingService == null) {
            return;
        }

        for (ICraftingCPU cpu : craftingService.getCpus()) {
            if (!cpu.isBusy()) {
                continue;
            }
            if (cpu instanceof CraftingCPUCluster cluster) {
                if (cluster.craftingLogic instanceof CraftingCpuLogicAccessor logicAccessor) {
                    var job = logicAccessor.extendedae_plus$getJob();
                    if (job instanceof ExecutingCraftingJobAccessor accessor) {
                        var tasks = accessor.extendedae_plus$getTasks();
                        var progress = tasks.get(patternDetails);
                        if (progress != null && progress.extendedae_plus$getValue() <= 1) {
                            cluster.updateOutput(null);
                            try {
                                logicAccessor.extendedae_plus$invokeFinishJob(true);
                            } catch (Throwable ignored) {
                                cluster.cancelJob();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean eap$compatIsVirtualCraftingEnabled() {
        return this.eap$compatVirtualCraftingEnabled;
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
        eap$compatTryVirtualCompletion(patternDetails);
    }

    @Override
    public void eap$updateWirelessLink() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }

        try {
            if (eap$compatLink != null) {
                eap$compatLink.updateStatus();
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性无线链接更新失败", e);
        }
    }

    @Override
    public void eap$initializeChannelLink() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }

        try {
            // 客户端早退
            if (host.getBlockEntity() != null && host.getBlockEntity().getLevel() != null && host.getBlockEntity().getLevel().isClientSide) {
                return;
            }

            // 避免重复初始化
            if (eap$compatHasInitialized) {
                return;
            }

            // 等待网格完成引导
            if (!mainNode.hasGridBooted()) {
                eap$compatDelayedInitTicks = Math.max(eap$compatDelayedInitTicks, 5);
                try {
                    mainNode.ifPresent((grid, node) -> {
                        try {
                            grid.getTickManager().wakeDevice(node);
                        } catch (Throwable ignored) {
                        }
                    });
                } catch (Throwable ignored) {
                }
                return;
            }

            long channel = 0L;
            boolean found = false;

            // 获取升级槽 - 如果装了appflux则从appflux获取，否则从我们自己的获取
            IUpgradeInventory upgrades = getUpgrades();

            if (upgrades != null) {
                for (ItemStack stack : upgrades) {
                    if (!stack.isEmpty() && stack.getItem() == ModItems.CHANNEL_CARD.get()) {
                        channel = ChannelCardItem.getChannel(stack);
                        java.util.UUID ownerUUID = ChannelCardItem.getOwnerUUID(stack);
                        if (ownerUUID != null) {
                            // 保存ownerUUID到局部变量，后面设置到link
                            channel |= ((long) ownerUUID.hashCode() << 32);  // 临时存储
                        }
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                // 无频道卡：断开并视为初始化完成
                if (eap$compatLink != null) {
                    eap$compatLink.setFrequency(0L);
                    eap$compatLink.updateStatus();
                }
                eap$compatHasInitialized = true;
                return;
            }

            if (eap$compatLink == null) {
                var endpoint = new GenericNodeEndpointImpl(() -> host.getBlockEntity(), () -> this.mainNode.getNode());
                eap$compatLink = new WirelessSlaveLink(endpoint);
            }

            // 从频道卡重新读取ownerUUID并设置
            java.util.UUID cardOwner = null;
            if (upgrades != null) {
                for (ItemStack stack : upgrades) {
                    if (!stack.isEmpty() && stack.getItem() == ModItems.CHANNEL_CARD.get()) {
                        cardOwner = ChannelCardItem.getOwnerUUID(stack);
                        channel = ChannelCardItem.getChannel(stack);  // 重新读取正确的频率
                        break;
                    }
                }
            }
            eap$compatLink.setPlacerId(cardOwner);
            eap$compatLink.setFrequency(channel);
            eap$compatLink.updateStatus();

            if (eap$compatLink.isConnected()) {
                eap$compatHasInitialized = true;
            } else {
                eap$compatHasInitialized = false;
                eap$compatDelayedInitTicks = Math.max(eap$compatDelayedInitTicks, 5);
                try {
                    mainNode.ifPresent((grid, node) -> {
                        try {
                            grid.getTickManager().wakeDevice(node);
                        } catch (Throwable ignored) {
                        }
                    });
                } catch (Throwable ignored) {
                }
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性频道链接初始化失败", e);
        }
    }

    @Override
    public void eap$setClientWirelessState(boolean connected) {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            eap$compatClientConnected = connected;
        }
    }

    @Override
    public boolean eap$isWirelessConnected() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return false;
        }

        try {
            if (host.getBlockEntity() != null && host.getBlockEntity().getLevel() != null && host.getBlockEntity().getLevel().isClientSide) {
                return eap$compatClientConnected;
            } else {
                return eap$compatLink != null && eap$compatLink.isConnected();
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("检查兼容性无线连接状态失败", e);
            return false;
        }
    }

    @Override
    public boolean eap$hasTickInitialized() {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            return eap$compatHasInitialized;
        }
        return true;
    }

    @Override
    public void eap$setTickInitialized(boolean initialized) {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            eap$compatHasInitialized = initialized;
        }
    }

    @Override
    public void eap$handleDelayedInit() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }

        try {
            // 仅服务端
            if (host.getBlockEntity() != null && host.getBlockEntity().getLevel() != null && host.getBlockEntity().getLevel().isClientSide) {
                return;
            }
            if (!eap$compatHasInitialized) {
                if (!mainNode.hasGridBooted()) {
                    if (eap$compatDelayedInitTicks > 0) {
                        eap$compatDelayedInitTicks--;
                    }
                    if (eap$compatDelayedInitTicks == 0) {
                        eap$compatDelayedInitTicks = 5;
                        try {
                            mainNode.ifPresent((grid, node) -> {
                                try {
                                    grid.getTickManager().wakeDevice(node);
                                } catch (Throwable ignored) {
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }
                } else {
                    eap$initializeChannelLink();
                    eap$syncVirtualCraftingState();
                }
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性延迟初始化失败", e);
        }
    }

    @Inject(method = "onMainNodeStateChanged", at = @At("TAIL"))
    private void eap$compatOnMainNodeStateChangedTail(CallbackInfo ci) {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }

        try {
            eap$resetChannel();
            eap$compatDelayedInitTicks = 10;
            try {
                mainNode.ifPresent((grid, node) -> {
                    try {
                        grid.getTickManager().wakeDevice(node);
                    } catch (Throwable ignored) {
                    }
                });
            } catch (Throwable ignored) {
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性主节点状态变更处理失败", e);
        }
    }
}

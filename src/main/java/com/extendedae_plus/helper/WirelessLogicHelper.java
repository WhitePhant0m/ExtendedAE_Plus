package com.extendedae_plus.helper;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.extendedae_plus.ae.wireless.WirelessSlaveLink;
import com.extendedae_plus.ae.wireless.endpoint.GenericNodeEndpointImpl;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import com.extendedae_plus.init.ModItems;
import com.extendedae_plus.items.materials.ChannelCardItem;
import com.extendedae_plus.mixin.ae2.accessor.CraftingCpuLogicAccessor;
import com.extendedae_plus.mixin.ae2.accessor.ExecutingCraftingJobAccessor;
import com.extendedae_plus.util.Logger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class WirelessLogicHelper {

    private final Supplier<BlockEntity> beSupplier;
    private final IManagedGridNode mainNode;
    private final Supplier<IGrid> gridSupplier;
    private final Supplier<IUpgradeInventory> upgradeInventorySupplier;

    private WirelessSlaveLink link;
    private boolean hasInitialized = false;
    private int delayedInitTicks = 0;
    private boolean virtualCraftingEnabled = false;
    private boolean clientConnected = false;

    public WirelessLogicHelper(Supplier<BlockEntity> beSupplier, IManagedGridNode mainNode, Supplier<IGrid> gridSupplier, Supplier<IUpgradeInventory> upgradeInventorySupplier) {
        this.beSupplier = beSupplier;
        this.mainNode = mainNode;
        this.gridSupplier = gridSupplier;
        this.upgradeInventorySupplier = upgradeInventorySupplier;
    }

    public void syncVirtualCraftingState() {
        if (upgradeInventorySupplier == null) {
            return;
        }
        boolean hasCard = false;
        IUpgradeInventory inventory = upgradeInventorySupplier.get();
        if (inventory != null) {
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty() && stack.getItem() == ModItems.VIRTUAL_CRAFTING_CARD.get()) {
                    hasCard = true;
                    break;
                }
            }
        }
        virtualCraftingEnabled = hasCard;
    }

    public void resetChannel() {
        hasInitialized = false;
    }

    public void tryVirtualCompletion(IPatternDetails patternDetails) {
        if (!virtualCraftingEnabled || beSupplier == null || gridSupplier == null) {
            return;
        }

        var be = beSupplier.get();
        if (be == null || be.getLevel() == null || be.getLevel().isClientSide) {
            return;
        }

        var grid = gridSupplier.get();
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

    public boolean isVirtualCraftingEnabled() {
        return virtualCraftingEnabled;
    }

    public void updateWirelessLink() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }

        try {
            if (link != null) {
                link.updateStatus();
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性无线链接更新失败", e);
        }
    }

    public void initializeChannelLink() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard() || beSupplier == null || mainNode == null) {
            return;
        }

        try {
            var be = beSupplier.get();
            // 客户端早退
            if (be != null && be.getLevel() != null && be.getLevel().isClientSide) {
                return;
            }

            // 避免重复初始化
            if (hasInitialized) {
                return;
            }

            // 等待网格完成引导
            if (!mainNode.isReady()) {
                delayedInitTicks = Math.max(delayedInitTicks, 5);
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

            if (upgradeInventorySupplier == null) {
                return;
            }
            IUpgradeInventory upgrades = upgradeInventorySupplier.get();

            if (upgrades != null) {
                for (ItemStack stack : upgrades) {
                    if (!stack.isEmpty() && stack.getItem() == ModItems.CHANNEL_CARD.get()) {
                        channel = ChannelCardItem.getChannel(stack);
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                // 无频道卡：断开并视为初始化完成
                if (link != null) {
                    link.setFrequency(0L);
                    link.updateStatus();
                }
                hasInitialized = true;
                return;
            }

            if (link == null) {
                var endpoint = new GenericNodeEndpointImpl(beSupplier, () -> mainNode.getNode());
                link = new WirelessSlaveLink(endpoint);
            }

            // 从频道卡重新读取ownerUUID并设置
            UUID cardOwner = null;
            if (upgrades != null) {
                for (ItemStack stack : upgrades) {
                    if (!stack.isEmpty() && stack.getItem() == ModItems.CHANNEL_CARD.get()) {
                        cardOwner = ChannelCardItem.getOwnerUUID(stack);
                        channel = ChannelCardItem.getChannel(stack);  // 重新读取正确的频率
                        break;
                    }
                }
            }
            link.setPlacerId(cardOwner);
            link.setFrequency(channel);
            link.updateStatus();

            if (link.isConnected()) {
                hasInitialized = true;
            } else {
                hasInitialized = false;
                delayedInitTicks = Math.max(delayedInitTicks, 5);
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

    public void setClientWirelessState(boolean connected) {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            clientConnected = connected;
        }
    }

    public boolean isWirelessConnected() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard() || beSupplier == null) {
            return false;
        }

        try {
            var be = beSupplier.get();
            if (be != null && be.getLevel() != null && be.getLevel().isClientSide) {
                return clientConnected;
            } else {
                return link != null && link.isConnected();
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("检查兼容性无线连接状态失败", e);
            return false;
        }
    }

    public boolean hasTickInitialized() {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            return hasInitialized;
        }
        return true;
    }

    public void setTickInitialized(boolean initialized) {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            hasInitialized = initialized;
        }
    }

    public void handleDelayedInit() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard() || beSupplier == null || mainNode == null) {
            return;
        }

        try {
            var be = beSupplier.get();
            // 仅服务端
            if (be != null && be.getLevel() != null && be.getLevel().isClientSide) {
                return;
            }
            if (!hasInitialized) {
                if (!mainNode.isReady()) {
                    if (delayedInitTicks > 0) {
                        delayedInitTicks--;
                    }
                    if (delayedInitTicks == 0) {
                        delayedInitTicks = 5;
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
                    initializeChannelLink();
                    syncVirtualCraftingState();
                }
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性延迟初始化失败", e);
        }
    }

    public void onMainNodeStateChanged() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard() || mainNode == null) {
            return;
        }

        try {
            resetChannel();
            delayedInitTicks = 10;
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

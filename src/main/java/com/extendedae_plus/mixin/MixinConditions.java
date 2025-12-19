package com.extendedae_plus.mixin;

import com.extendedae_plus.util.ModCheckUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin条件加载插件
 * 用于根据模组存在情况动态加载不同的Mixin
 */
public class MixinConditions implements IMixinConfigPlugin {
    private static final Object2ObjectMap<String, List<String>> loadIfPresent = new Object2ObjectOpenHashMap<>(
            new String[]{
                    "com.extendedae_plus.mixin.advancedae",
                    "com.extendedae_plus.mixin.appflux.UpgradeSlotMixin"
            },
            new List[]{
                    List.of("advanced_ae"),
                    List.of("appflux")
            }
    );
    private static final Object2ObjectMap<String, List<String>> loadIfNotPresent = new Object2ObjectOpenHashMap<>(
            new String[]{
                    "com.extendedae_plus.mixin.ae2.CraftingCPUClusterMixin",
                    "com.extendedae_plus.mixin.ae2.client.gui.CraftConfirmScreenMixin",
                    "com.extendedae_plus.mixin.ae2.compat.PatternProviderLogicHostCompatMixin",
                    "com.extendedae_plus.mixin.ae2.compat.PatternProviderCompatMixin",
                    "com.extendedae_plus.mixin.ae2.compat.PatternProviderScreenCompatMixin"
            },
            new List[]{
                    List.of("mae2"),
                    List.of("expandedae"),
                    List.of("expandedae", "appflux", "pccard"),
                    List.of("expandedae", "appflux", "pccard"),
                    List.of("expandedae", "appflux", "pccard")
            }
    );

    private boolean isModLoaded(String modId) {
        if (ModList.get() == null) {
            return LoadingModList.get().getMods().stream()
                    .map(ModInfo::getModId)
                    .anyMatch(modId::equals);
        } else {
            return ModList.get().isLoaded(modId);
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        for (var entry : loadIfPresent.entrySet()) {
            if (mixinClassName.startsWith(entry.getKey())) {
                return entry.getValue().stream().anyMatch(this::isModLoaded);
            }
        }

        for (var entry : loadIfNotPresent.entrySet()) {
            if (mixinClassName.startsWith(entry.getKey())) {
                return entry.getValue().stream().noneMatch(this::isModLoaded);
            }
        }

        // === GuideME 版本兼容 ===
        if (mixinClassName.startsWith("com.extendedae_plus.mixin.guideme.")) {
            return ModCheckUtils.isLoadedAndLowerThan(ModCheckUtils.MODID_GUIDEME, "20.1.14");
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // 接受目标类
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 应用前调用
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 应用后调用
    }
}

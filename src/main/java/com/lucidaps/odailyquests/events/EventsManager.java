package com.lucidaps.odailyquests.events;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.CustomFurnaceResults;
import com.lucidaps.odailyquests.configuration.integrations.NexoEnabled;
import com.lucidaps.odailyquests.events.listeners.customs.CustomFurnaceExtractListener;
import com.lucidaps.odailyquests.events.listeners.entity.*;
import com.lucidaps.odailyquests.events.listeners.global.*;
import com.lucidaps.odailyquests.events.listeners.integrations.npcs.CitizensHook;
import com.lucidaps.odailyquests.events.listeners.integrations.nexo.NexoItemsLoadedListener;
import com.lucidaps.odailyquests.events.listeners.item.*;
import com.lucidaps.odailyquests.events.listeners.item.custom.DropQueuePushListener;
import com.lucidaps.odailyquests.events.listeners.inventory.InventoryClickListener;
import com.lucidaps.odailyquests.events.listeners.inventory.InventoryCloseListener;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.PluginUtils;
import com.lucidaps.odailyquests.events.listeners.vote.VotifierPlusListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class EventsManager {

    private final ODailyQuests oDailyQuests;

    public EventsManager(ODailyQuests oDailyQuests) {
        this.oDailyQuests = oDailyQuests;
    }

    /**
     * Registers all events.
     */
    public void registerListeners() {
        final PluginManager pluginManager = Bukkit.getPluginManager();

        registerBukkitNativeListeners(pluginManager);
        registerCustomEvents(pluginManager);
        registerPackIntegrations(pluginManager);
        registerPluginListeners(pluginManager);
    }

    private void registerBukkitNativeListeners(final PluginManager pluginManager) {
        // entity events
        pluginManager.registerEvents(new EntityBreedListener(), oDailyQuests);
        pluginManager.registerEvents(new EntityTameListener(), oDailyQuests);
        pluginManager.registerEvents(new ShearEntityListener(), oDailyQuests);
        pluginManager.registerEvents(new EntityDeathListener(), oDailyQuests);
        pluginManager.registerEvents(new SpawnerSpawnListener(), oDailyQuests);

        // global events
        pluginManager.registerEvents(new BucketFillListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerExpChangeListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerLevelChangeListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerInteractListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerInteractEntityListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerDeathListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerRespawnListener(), oDailyQuests);

        // item events
        pluginManager.registerEvents(new BlockBreakListener(), oDailyQuests);
        pluginManager.registerEvents(new BlockPlaceListener(), oDailyQuests);
        pluginManager.registerEvents(new CraftItemListener(), oDailyQuests);
        pluginManager.registerEvents(new SmithItemListener(), oDailyQuests);
        pluginManager.registerEvents(new EnchantItemListener(), oDailyQuests);
        pluginManager.registerEvents(new FurnaceExtractListener(), oDailyQuests);
        pluginManager.registerEvents(new PickupItemListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerFishListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerItemConsumeListener(), oDailyQuests);
        pluginManager.registerEvents(new ProjectileLaunchListener(), oDailyQuests);
        pluginManager.registerEvents(new InventoryClickListener(oDailyQuests.getInterfacesManager().getPlayerQuestsInterface()), oDailyQuests);
        pluginManager.registerEvents(new BlockDropItemListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerHarvestBlockListener(), oDailyQuests);
        pluginManager.registerEvents(new PlayerDropItemListener(), oDailyQuests);
        pluginManager.registerEvents(new StructureGrowListener(), oDailyQuests);

        // inventory events
        pluginManager.registerEvents(new InventoryCloseListener(), oDailyQuests);
    }

    private void registerCustomEvents(final PluginManager pluginManager) {
        if (NexoEnabled.isEnabled() || CustomFurnaceResults.isEnabled()) {
            pluginManager.registerEvents(new CustomFurnaceExtractListener(), oDailyQuests);
        }
    }

    private void registerPackIntegrations(final PluginManager pluginManager) {
        if (NexoEnabled.isEnabled()) {
            registerSafely(() -> pluginManager.registerEvents(new NexoItemsLoadedListener(oDailyQuests), oDailyQuests), "Nexo");
        }
    }

    private void registerPluginListeners(final PluginManager pluginManager) {
        // Third-party plugin hooks with safety wrapper
        registerIfPluginEnabled("Citizens", () -> pluginManager.registerEvents(new CitizensHook(oDailyQuests.getInterfacesManager()), oDailyQuests));
        registerIfPluginEnabled("VotifierPlus", () -> VotifierPlusListener.register(pluginManager, oDailyQuests));
        registerIfPluginEnabled("eco", () -> pluginManager.registerEvents(new DropQueuePushListener(), oDailyQuests));
    }

    private void registerIfPluginEnabled(final String pluginName, final Runnable registerAction) {
        if (!PluginUtils.isPluginEnabled(pluginName)) return;
        registerSafely(registerAction, pluginName);
    }

    private void registerSafely(final Runnable registerAction, final String prettyName) {
        try {
            registerAction.run();
        } catch (NoClassDefFoundError e) {
            PluginLogger.warn("Cannot hook into " + prettyName + " events. This is usually caused by an outdated " + prettyName + " version.");
            PluginLogger.warn("If the problem persists, please contact the plugin developer.");
        }
    }
}

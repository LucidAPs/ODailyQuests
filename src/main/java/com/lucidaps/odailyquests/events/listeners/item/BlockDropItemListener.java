package com.lucidaps.odailyquests.events.listeners.item;

import com.lucidaps.odailyquests.configuration.essentials.Antiglitch;
import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.events.listeners.item.custom.DropQueuePushListener;
import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import com.lucidaps.odailyquests.tools.PluginUtils;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BlockDropItemListener extends PlayerProgressor implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Debugger.write("BlockDropItemListener: onBlockDropItemEvent summoned.");

        if (event.isCancelled()) {
            Debugger.write("BlockDropItemListener: onBlockDropItemEvent is cancelled.");
            return;
        }

        final Player player = event.getPlayer();
        final BlockData data = event.getBlockState().getBlockData();
        final Material dataMaterial = data.getMaterial();

        if (isVerticalPlant(dataMaterial)) {
            Debugger.write("BlockDropItemListener: onBlockDropItemEvent vertical plant detected, skipping drop handling.");
            return;
        }

        // fix attempt for eco plugins compatibility issue
        if (PluginUtils.isPluginEnabled("eco")) {
            Debugger.write("BlockDropItemListener: onBlockDropItemEvent eco is enabled. Setting current state for DropQueuePushListener.");
            DropQueuePushListener.setCurrentState(event.getBlockState());
        }

        Debugger.write("BlockDropItemListener: onBlockDropItemEvent block data: " + dataMaterial.name() + ".");

        final List<Item> drops = event.getItems();
        if (drops.isEmpty()) {
            Debugger.write("BlockDropItemListener: onBlockDropItemEvent no drops.");
            return;
        }

        // check if the dropped item is a crop
        if (isAgeableAndHandleOrBlock(event, data, dataMaterial, player, drops)) return;

        // check if the block have been placed by the player
        if (isPlayerPlacedBlock(event.getBlock(), dataMaterial)) return;

        // handle remaining drops
        handleDrops(event, player, drops);

        // check if the dropped item is a block that can be posed
        handleStoreBrokenBlocks(drops, player, dataMaterial);
    }

    /**
     * Handles ageable blocks (crops) to avoid quest progression when they are not fully grown.
     *
     * <p>
     * If the broken block is {@link Ageable} and not mature, this method blocks further processing.
     * If it is mature, drops are handled here.
     * Non-ageable blocks (and vertical plants) are ignored and handled by the normal flow.
     *
     * @param event        the event that triggered the listener
     * @param data         the block data of the broken block
     * @param dataMaterial the material of the broken block
     * @param player       the player who broke the block
     * @param drops        list of dropped {@link Item} entities
     * @return {@code true} if processing should stop, {@code false} otherwise
     */
    private boolean isAgeableAndHandleOrBlock(Event event, BlockData data, Material dataMaterial, Player player, List<Item> drops) {
        if (isVerticalPlant(dataMaterial)) {
            Debugger.write("BlockDropItemListener: vertical plant detected, skipping ageable check.");
            return false; // skip vertical plants
        }

        if (!(data instanceof Ageable ageable)) {
            return false; // not ageable => let the normal flow handle drops
        }

        Debugger.write("BlockDropItemListener: ageable block: " + dataMaterial + " age=" + ageable.getAge() + "/" + ageable.getMaximumAge());

        if (ageable.getAge() < ageable.getMaximumAge()) {
            Debugger.write("BlockDropItemListener: ageable not mature -> blocking progression.");
            return true; // ageable but not mature => stop here, don't progress quests
        }

        Debugger.write("BlockDropItemListener: ageable mature -> handling drops.");
        handleDrops(event, player, drops); // ageable and mature => handled, stop here to avoid double handling
        return true;
    }

    /**
     * Converts dropped in-world items to {@link ItemStack}s and stores metadata
     * indicating the block was broken by the specified player.
     * <p>
     * This method is used in event contexts where drops are {@link Item} entities,
     * such as {@link org.bukkit.event.block.BlockDropItemEvent}.
     *
     * @param drops    the list of dropped {@link Item} entities from the event
     * @param player   the player who broke the block
     * @param material the material of the block that was broken
     */
    private void handleStoreBrokenBlocks(List<Item> drops, Player player, Material material) {
        if (material.isBlock() && Antiglitch.isStoreBrokenBlocks()) {
            Debugger.write("BlockDropItemListener: onBlockDropItemEvent storing broken block.");

            final List<ItemStack> itemStacks = drops.stream()
                    .map(Item::getItemStack)
                    .toList();

            storeBrokenBlockMetadata(itemStacks, player);
        }
    }
}

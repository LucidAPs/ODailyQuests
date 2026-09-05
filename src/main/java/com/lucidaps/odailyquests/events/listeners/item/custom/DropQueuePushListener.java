package com.lucidaps.odailyquests.events.listeners.item.custom;

import com.lucidaps.odailyquests.configuration.essentials.Antiglitch;
import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import com.lucidaps.odailyquests.quests.types.item.FarmingQuest;
import com.willfp.eco.core.events.DropQueuePushEvent;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class DropQueuePushListener extends PlayerProgressor implements Listener {

    private static BlockState currentState;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropQueuePush(DropQueuePushEvent event) {
        Debugger.write("DropQueuePushListener: onDropQueuePush event summoned.");

        if (event.isCancelled()) {
            Debugger.write("DropQueuePushListener: onDropQueuePush event is cancelled.");
            return;
        }

        if (currentState == null) {
            Debugger.write("DropQueuePushListener: onDropQueuePush event currentState is null.");
            return;
        }

        final Player player = event.getPlayer();
        final BlockData data = currentState.getBlockData();
        final Material dataMaterial = data.getMaterial();

        Debugger.write("DropQueuePushListener: onDropQueuePush event triggered by player: " + player.getName() + ".");

        final Collection<? extends ItemStack> drops = event.getItems();
        if (drops.isEmpty()) {
            Debugger.write("DropQueuePushListener: onDropQueuePush event no drops.");
            return;
        }

        // check if the dropped item is a crop
        if (isAgeableAndHandleOrBlock(event, data, dataMaterial, drops)) return;

        // check if the block have been placed by the player
        if (isPlayerPlacedBlock(currentState.getBlock(), dataMaterial)) return;

        // handle remaining drops
        handleDrops(event, drops);

        // check if the dropped item is a block that can be posed
        handleStoreBrokenBlocks(drops, player, dataMaterial);
    }

    /**
     * If the block is {@link Ageable}, either handles drops (when mature) or blocks progression (when not mature).
     *
     * @param event        the event that triggered the listener
     * @param data         the block data of the broken block
     * @param dataMaterial the material of the block data
     * @param drops        list of dropped items
     * @return true if the block is ageable (mature or not) and the caller must stop processing, false otherwise
     */
    private boolean isAgeableAndHandleOrBlock(DropQueuePushEvent event, BlockData data, Material dataMaterial, Collection<? extends ItemStack> drops) {
        if (!(data instanceof Ageable ageable)) {
            return false; // not ageable => let the normal flow handle drops
        }

        Debugger.write("DropQueuePushListener: ageable block: " + dataMaterial + " age=" + ageable.getAge() + "/" + ageable.getMaximumAge());

        if (ageable.getAge() < ageable.getMaximumAge()) {
            Debugger.write("DropQueuePushListener: ageable block is not mature, blocking drop handling.");
            return true; // ageable but not mature => stop here, don't progress quests
        }

        Debugger.write("DropQueuePushListener: ageable block is mature, handling drops.");
        handleDrops(event, drops);
        return true; // ageable and mature => handled, stop here to avoid double handling
    }
    /**
     * Handle the dropped items and update the player progression.
     *
     * @param event  the event that triggered the listener
     * @param drops  list of dropped items
     */
    private void handleDrops(DropQueuePushEvent event, Collection<? extends ItemStack> drops) {
        Debugger.write("DropQueuePushListener: handleDrops summoned.");
        for (ItemStack item : drops) {
            final Material droppedMaterial = item.getType();
            Debugger.write("DropQueuePushListener: handling drop: " + droppedMaterial + ".");

            FarmingQuest.setCurrent(new ItemStack(droppedMaterial));
            setPlayerQuestProgression(event, event.getPlayer(), item.getAmount(), "FARMING");
        }
    }

    /**
     * Stores metadata on a collection of dropped {@link ItemStack}s to mark them
     * as broken by the specified player.
     * <p>
     * This version is used when the drops are already {@link ItemStack}s, such as in
     * virtual drop systems like {@link DropQueuePushEvent}.
     *
     * @param drops    the collection of dropped {@link ItemStack}s
     * @param player   the player who broke the block
     * @param material the material of the block that was broken
     */
    private void handleStoreBrokenBlocks(Collection<? extends ItemStack> drops, Player player, Material material) {
        if (material.isBlock() && Antiglitch.isStoreBrokenBlocks()) {
            Debugger.write("DropQueuePushListener: onBlockDropItemEvent storing broken block.");
            storeBrokenBlockMetadata(drops, player);
        }
    }

    public static void setCurrentState(BlockState currentState) {
        DropQueuePushListener.currentState = currentState;
    }

    public static BlockState getCurrentState() {
        return currentState;
    }
}

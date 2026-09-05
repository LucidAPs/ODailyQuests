package com.lucidaps.odailyquests.quests.types.item;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.externs.hooks.Protection;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.quests.types.shared.ItemQuest;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Quest implementation triggered when a player places a block.
 * <p>
 * This quest progresses when the player places a block matching
 * the required item defined by the quest configuration.
 * Protection checks are applied unless the quest explicitly bypasses them.
 */
public class PlaceQuest extends ItemQuest {

    /**
     * Creates a new block placement quest.
     *
     * @param base the base quest configuration used to initialize this quest
     */
    public PlaceQuest(BasicQuest base) {
        super(base);
    }

    /**
     * Returns the unique quest type identifier.
     *
     * @return the quest type string, always "PLACE"
     */
    @Override
    public String getType() {
        return "PLACE";
    }

    /**
     * Determines whether this quest can progress for the given event.
     * <p>
     * Progression is allowed only when:
     * - the provided event is a block placement event
     * - the player is allowed to place the block (protection check)
     * - the placed block matches the required quest item
     *
     * @param provided    the event triggering the progression check
     * @param progression the player's current quest progression
     * @return true if the quest should progress, false otherwise
     */
    @Override
    public boolean canProgress(Event provided, Progression progression) {
        if (provided instanceof BlockPlaceEvent event) {
            final Block block = event.getBlock();
            if (!this.isProtectionBypass() && !Protection.canBuild(event.getPlayer(), block, "BLOCK_PLACE")) {
                return false;
            }

            final ItemStack placedItem = event.getItemInHand();

            Debugger.write("BlockPlaceListener: onBlockPlaceEvent summoned by " + event.getPlayer().getName() + " for " + placedItem.getType() + ".");
            return super.isRequiredItem(new ItemStack(placedItem.getType()), progression);
        }

        return false;
    }
}

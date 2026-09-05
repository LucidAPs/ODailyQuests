package com.lucidaps.odailyquests.quests.types.item;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.quests.types.shared.ItemQuest;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Quest implementation triggered when a player catches an item while fishing.
 * <p>
 * This quest progresses from vanilla Bukkit fishing events.
 * <p>
 * The quest progresses when the caught item matches the required quest item.
 */
public class FishQuest extends ItemQuest {

    /**
     * Creates a new fishing quest.
     *
     * @param base the base quest configuration used to initialize this quest
     */
    public FishQuest(BasicQuest base) {
        super(base);
    }

    /**
     * Returns the unique quest type identifier.
     *
     * @return the quest type string, always "FISH"
     */
    @Override
    public String getType() {
        return "FISH";
    }

    /**
     * Determines whether this quest can progress for the given event.
     * <p>
     * Progression is allowed when the event is a vanilla PlayerFishEvent with a caught item
     * and the caught item matches the required quest item.
     *
     * @param provided    the event triggering the progression check
     * @param progression the player's current quest progression
     * @return true if the quest should progress, false otherwise
     */
    @Override
    public boolean canProgress(Event provided, Progression progression) {
        if (provided instanceof PlayerFishEvent event) {
            final Item item = (Item) event.getCaught();
            if (item == null) return false;
            return super.isRequiredItem(item.getItemStack(), progression);
        }

        return false;
    }
}

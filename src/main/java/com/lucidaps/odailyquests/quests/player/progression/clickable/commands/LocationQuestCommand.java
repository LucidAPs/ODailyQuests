package com.lucidaps.odailyquests.quests.player.progression.clickable.commands;

import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.QuestCompletionHandler;
import com.lucidaps.odailyquests.quests.player.progression.clickable.QuestCommand;
import com.lucidaps.odailyquests.quests.player.progression.clickable.QuestContext;
import com.lucidaps.odailyquests.quests.types.inventory.LocationQuest;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationQuestCommand extends QuestCommand<LocationQuest> {

    public LocationQuestCommand(QuestContext context, Progression progression, LocationQuest quest) {
        super(context, progression, quest);
    }

    /**
     * Validate LOCATION quest type.
     */
    @Override
    public void execute() {
        final var player = context.getPlayer();
        if (!quest.isAllowedToProgress(player, quest)) return;

        final Location requiredLocation = quest.getRequiredLocation();
        final World requiredWorld = requiredLocation.getWorld();

        if (requiredWorld != null && !requiredWorld.equals(player.getLocation().getWorld())) {
            sendMessage(QuestsMessages.BAD_WORLD_LOCATION);
            return;
        }

        double distance = player.getLocation().distance(requiredLocation);
        if (distance <= quest.getRadius()) {
            QuestCompletionHandler.completeQuest(player, progression, quest);

            player.closeInventory();
        } else {
            sendMessage(QuestsMessages.TOO_FAR_FROM_LOCATION);
        }
    }
}

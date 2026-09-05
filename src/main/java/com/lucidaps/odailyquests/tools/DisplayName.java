package com.lucidaps.odailyquests.tools;

import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.quests.types.shared.EntityQuest;
import com.lucidaps.odailyquests.quests.types.shared.ItemQuest;
import org.bukkit.ChatColor;

public class DisplayName {

    private DisplayName() {}

    public static String getDisplayName(AbstractQuest quest, int index) {
        if (!quest.isRandomRequired()) {
            return ChatColor.RED + "Invalid usage.";
        }

        String displayName = null;
        if (quest instanceof EntityQuest entityQuest) {
            displayName = entityQuest.getSelectedDisplayName(index);
        } else if (quest instanceof ItemQuest itemQuest) {
            displayName = itemQuest.getSelectedDisplayName(index);
        }

        if (displayName == null) return ChatColor.RED + "Invalid usage.";
        return displayName;
    }
}

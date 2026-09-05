package com.lucidaps.odailyquests.quests.player.progression.clickable;

import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.quests.player.progression.QuestCompletionHandler;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;

public abstract class QuestCommand<Q extends AbstractQuest> {
    protected final QuestContext context;
    protected final Progression progression;
    protected final Q quest;

    protected QuestCommand(QuestContext context, Progression progression, Q quest) {
        this.context = context;
        this.progression = progression;
        this.quest = quest;
    }

    /**
     * Validate and complete the quest.
     */
    public abstract void execute();

    /**
     * Send a message to the player if the message is defined.
     *
     * @param message the message enum.
     */
    protected void sendMessage(QuestsMessages message) {
        String msg = message.getMessage(context.getPlayer());
        if (msg != null) context.getPlayer().sendMessage(msg);
    }

    /**
     * Complete the quest.
     */
    protected void completeQuest() {
        QuestCompletionHandler.completeQuest(context.getPlayer(), progression, quest);
        context.getPlayer().closeInventory();
    }
}

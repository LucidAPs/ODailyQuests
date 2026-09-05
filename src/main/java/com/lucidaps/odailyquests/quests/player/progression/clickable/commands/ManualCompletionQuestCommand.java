package com.lucidaps.odailyquests.quests.player.progression.clickable.commands;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.clickable.QuestCommand;
import com.lucidaps.odailyquests.quests.player.progression.clickable.QuestContext;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;

public class ManualCompletionQuestCommand extends QuestCommand<AbstractQuest> {

    public ManualCompletionQuestCommand(QuestContext context, Progression progression, AbstractQuest quest) {
        super(context, progression, quest);
    }

    @Override
    public void execute() {
        if (progression.getAdvancement() < progression.getRequiredAmount()) {
            return;
        }

        completeQuest();
    }
}

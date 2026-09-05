package com.lucidaps.odailyquests.quests.player.progression.clickable.commands;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.quests.conditions.ConditionOperator;
import com.lucidaps.odailyquests.quests.conditions.placeholder.PlaceholderConditionEvaluator;
import com.lucidaps.odailyquests.quests.conditions.placeholder.PlaceholderConditionResult;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.clickable.QuestCommand;
import com.lucidaps.odailyquests.quests.player.progression.clickable.QuestContext;
import com.lucidaps.odailyquests.quests.types.inventory.PlaceholderQuest;
import com.lucidaps.odailyquests.tools.TextFormatter;
import org.bukkit.entity.Player;

/**
 * Command implementation for handling quests of type {@code PLACEHOLDER}.
 * <p>
 * This command evaluates a placeholder-based condition for the executing player.
 * It checks whether PlaceholderAPI is available, resolves the placeholder and
 * expected values, and compares them according to the quest's {@link ConditionOperator}.
 * </p>
 * <p>
 * If the condition matches, the quest is completed. If the condition cannot be
 * evaluated due to invalid input format, or if it does not match, the player
 * receives appropriate feedback messages defined by the quest configuration.
 */
public class PlaceholderQuestCommand extends QuestCommand<PlaceholderQuest> {

    /**
     * Creates a new command for evaluating a placeholder quest.
     *
     * @param context     the quest context providing execution details
     * @param progression the current progression of the player
     * @param quest       the placeholder quest being executed
     */
    public PlaceholderQuestCommand(QuestContext context, Progression progression, PlaceholderQuest quest) {
        super(context, progression, quest);
    }

    /**
     * Executes the validation of a placeholder quest.
     * <ul>
     *   <li>Ensures the player is allowed to progress in the quest.</li>
     *   <li>Verifies that PlaceholderAPI is enabled.</li>
     *   <li>Resolves and compares the placeholder value with the expected value.</li>
     *   <li>Completes the quest if the condition is satisfied, or displays an
     *       error message otherwise.</li>
     * </ul>
     */
    @Override
    public void execute() {
        final Player player = context.getPlayer();
        Debugger.write("Executing PlaceholderQuestCommand for player " + player.getName());

        if (!quest.isAllowedToProgress(player, quest)) return;

        if (!TextFormatter.isPlaceholderAPIEnabled()) {
            sendMessage(QuestsMessages.PLACEHOLDER_API_NOT_ENABLED);
            return;
        }

        Debugger.write("Placeholders before formatting for player " + player.getName() + ": placeholder='" + quest.getPlaceholder() + "', expectedValue='" + quest.getExpectedValue() + "'");
        final String placeholderValue = TextFormatter.format(player, quest.getPlaceholder());
        final String expectedValue = TextFormatter.format(player, quest.getExpectedValue());
        Debugger.write("Evaluating placeholder condition for player " + player.getName() + ": placeholderValue='" + placeholderValue + "', expectedValue='" + expectedValue + "'");

        final PlaceholderConditionResult result = PlaceholderConditionEvaluator.evaluate(quest.getConditionType(), placeholderValue, expectedValue);

        if (result.invalidFormat()) {
            Debugger.write("Invalid format for placeholder value '" + placeholderValue + "' for player " + player.getName());
            handleValidationError(placeholderValue);
            return;
        }

        if (result.matched()) {
            Debugger.write("Placeholder condition matched for player " + player.getName() + ". Completing quest.");
            completeQuest();
        } else {
            Debugger.write("Placeholder condition did not match for player " + player.getName() + ".");
            final String message = TextFormatter.format(player, quest.getErrorMessage());
            if (message != null && !message.isEmpty()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Handles the case where a placeholder value could not be parsed
     * into a valid format (e.g., a number or duration).
     * Displays a predefined error message to the player.
     *
     * @param placeholder the invalid placeholder value
     */
    private void handleValidationError(String placeholder) {
        String message = QuestsMessages.PLACEHOLDER_NOT_NUMBER.toString();
        if (message != null) {
            message = message.replace("%placeholder%", placeholder);
            context.getPlayer().sendMessage(message);
        }
    }
}

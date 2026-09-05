package com.lucidaps.odailyquests.events.listeners.vote;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;

public class VotifierPlusListener extends PlayerProgressor implements Listener {

    private static final String VOTIFIER_EVENT_CLASS = "com.vexsoftware.votifier.model.VotifierEvent";
    private boolean reflectionWarningLogged;

    public static void register(PluginManager pluginManager, ODailyQuests plugin) {
        final Plugin votifierPlus = pluginManager.getPlugin("VotifierPlus");
        if (votifierPlus == null || !votifierPlus.isEnabled()) return;

        final Class<? extends Event> votifierEventClass = resolveVotifierEventClass(votifierPlus);
        if (votifierEventClass == null) {
            PluginLogger.warn("Cannot hook into VotifierPlus events. The VotifierPlus vote event class was not found.");
            return;
        }

        final VotifierPlusListener listener = new VotifierPlusListener();
        pluginManager.registerEvent(votifierEventClass, listener, EventPriority.NORMAL,
                (registeredListener, event) -> listener.onVotifierPlusEvent(event), plugin);
    }

    private static Class<? extends Event> resolveVotifierEventClass(Plugin votifierPlus) {
        try {
            return Class.forName(VOTIFIER_EVENT_CLASS, false, votifierPlus.getClass().getClassLoader()).asSubclass(Event.class);
        } catch (ClassNotFoundException | ClassCastException | LinkageError exception) {
            return null;
        }
    }

    private void onVotifierPlusEvent(Event event) {
        final String username = getVoteUsername(event);
        if (username == null || username.isBlank()) return;

        final Player player = Bukkit.getPlayerExact(username);
        if (player == null) return;

        setPlayerQuestProgression(event, player, 1, "VOTIFIER_PLUS");
    }

    private String getVoteUsername(Event event) {
        try {
            final Method getVote = event.getClass().getMethod("getVote");
            final Object vote = getVote.invoke(event);
            if (vote == null) return null;

            final Method getUsername = vote.getClass().getMethod("getUsername");
            final Object username = getUsername.invoke(vote);
            return username instanceof String value ? value : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!reflectionWarningLogged) {
                PluginLogger.warn("Cannot read VotifierPlus vote data. Vote quest progression will be ignored.");
                reflectionWarningLogged = true;
            }
            return null;
        }
    }
}

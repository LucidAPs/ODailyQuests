package com.lucidaps.odailyquests.quests.types.item;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.externs.hooks.Protection;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.quests.types.shared.ItemQuest;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BreakQuest extends ItemQuest {

    public BreakQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return "BREAK";
    }

    @Override
    public boolean canProgress(Event provided, Progression progression) {
        if (provided instanceof BlockBreakEvent event) {
            if (event.isCancelled()) return false;

            final Block block = event.getBlock();

            if (!this.isProtectionBypass() && !Protection.canBuild(event.getPlayer(), block, "BLOCK_BREAK"))
                return false;

            Debugger.write("BlockBreakListener: onBlockBreakEvent summoned by " + event.getPlayer().getName() + " for " + block.getType() + ".");

            Material material = switch (block.getType()) {
                case POTATOES -> Material.POTATO;
                case CARROTS -> Material.CARROT;
                case BEETROOTS -> Material.BEETROOT;
                case COCOA -> Material.COCOA_BEANS;
                case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
                default -> block.getType();
            };

            if (!material.isItem()) {
                Debugger.write("BreakQuest: canProgress material is not an item: " + material);
                Debugger.write("BreakQuest: cancelling event.");
                return false;
            }

            Debugger.write("BreakQuest: canProgress material: " + material);
            return super.isRequiredItem(new ItemStack(material), progression);
        }

        return false;
    }
}

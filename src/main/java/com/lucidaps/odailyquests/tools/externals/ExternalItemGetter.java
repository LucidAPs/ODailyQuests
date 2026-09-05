package com.lucidaps.odailyquests.tools.externals;

import com.nexomc.nexo.api.NexoItems;
import com.lucidaps.odailyquests.configuration.integrations.NexoEnabled;
import com.lucidaps.odailyquests.tools.ItemUtils;
import com.lucidaps.odailyquests.tools.Pair;
import org.bukkit.inventory.ItemStack;

public abstract class ExternalItemGetter implements IExternalItemGetter {

    /**
     * Get a Nexo item by its namespace.
     *
     * @param namespace the namespace of the item
     * @return the ItemStack or null if it does not exist
     */
    @Override
    public Pair<String, ItemStack> getNexoItem(String namespace) {
        if (!NexoEnabled.isEnabled()) {
            return new Pair<>("Cannot find Nexo. Is use_nexo enabled in config?", null);
        }

        if (!NexoItems.exists(namespace)) {
            return new Pair<>("The item " + namespace + " does not exist in Nexo.", null);
        }

        return new Pair<>("", NexoItems.itemFromId(namespace).build());
    }

    /**
     * Get a custom head by its texture.
     *
     * @param texture the texture of the head
     * @return the ItemStack textured or not
     */
    @Override
    public Pair<String, ItemStack> getCustomHead(String texture) {
        return new Pair<>("", ItemUtils.getCustomHead(texture));
    }
}

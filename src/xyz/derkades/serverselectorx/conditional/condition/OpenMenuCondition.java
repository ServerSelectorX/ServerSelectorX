package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.util.Map;

public class OpenMenuCondition extends Condition {

    OpenMenuCondition() {
        super("open-menu");
    }

    @Override
    public boolean isTrue(Player player, Map<String, Object> options) {
        // When a player does not have any open menu, in creative it returns "creative" and in survival it returns "crafting"
        return player.getOpenInventory().getType() != InventoryType.CRAFTING &&
                player.getOpenInventory().getType() != InventoryType.CREATIVE;
    }

}

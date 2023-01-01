package xyz.derkades.serverselectorx.conditional.condition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import xyz.derkades.serverselectorx.InvisibilityToggle;

import java.util.Map;

public class HasHiddenOthersCondition extends Condition {

    HasHiddenOthersCondition() {
        super("has-hidden-others");
    }

    @Override
    public boolean isTrue(Player player, Map<String, Object> options) throws InvalidConfigurationException {
        return InvisibilityToggle.hasHiddenOthers(player);
    }

}

package com.robinmc.serverselectorx.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.robinmc.serverselectorx.Main;

import net.md_5.bungee.api.ChatColor;
 
public class IconMenu implements Listener {
	
    private String name;
    private int size;
    private OptionClickEventHandler handler;
    private Plugin plugin = Main.getPlugin();
    private Player player;
 
    private String[] optionNames;
    private ItemStack[] optionIcons;
 
    public IconMenu(String name, int size, OptionClickEventHandler handler) {
        this.name = name;
        this.size = size;
        this.handler = handler;
        this.optionNames = new String[size];
        this.optionIcons = new ItemStack[size];
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
 
    public IconMenu setOption(int position, ItemStack icon, String name, String... info) {
        optionNames[position] = name;
        optionIcons[position] = setItemNameAndLore(icon, name, info);
        return this;
    }
 
    public void setSpecificTo(Player player) {
        this.player = player;
    }
 
    public boolean isSpecific() {
        return player != null;
    }
 
    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, size, name);
        for (int i = 0; i < optionIcons.length; i++) {
            if (optionIcons[i] != null) {
                inventory.setItem(i, optionIcons[i]);
            }
        }
        player.openInventory(inventory);
    }
 
    public void destroy() {
        HandlerList.unregisterAll(this);
        handler = null;
        plugin = null;
        optionNames = null;
        optionIcons = null;
    }
 
	@EventHandler(priority = EventPriority.HIGHEST)
    void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getTitle().equals(name) && (player == null || event.getWhoClicked() == player)) {
            event.setCancelled(true);
            if (event.getClick() != ClickType.LEFT)
                return;
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < size && optionNames[slot] != null) {
                Plugin plugin = this.plugin;
                OptionClickEvent e = new OptionClickEvent((Player) event.getWhoClicked(), slot, optionNames[slot], optionIcons[slot]);
                handler.onOptionClick(e);
                ((Player) event.getWhoClicked()).updateInventory();
                if (e.willClose()) {
                    final Player p = (Player) event.getWhoClicked();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                           p.closeInventory();
                        }
                    });
                }
                if (e.willDestroy()) {
                    destroy();
                }
            }
        }
    }
 
    public interface OptionClickEventHandler {
        public void onOptionClick(OptionClickEvent event);
    }
 
    public class OptionClickEvent {
        private Player player;
        private int position;
        private String name;
        private boolean close;
        private boolean destroy;
        private ItemStack item;
 
        public OptionClickEvent(Player player, int position, String name, ItemStack item) {
            this.player = player;
            this.position = position;
            this.name = name;
            this.close = true;
            this.destroy = false;
            this.item = item;
        }
 
        public Player getPlayer() {
            return player;
        }
 
        public int getPosition() {
            return position;
        }
 
        public String getName() {
            return name;
        }
 
        public boolean willClose() {
            return close;
        }
 
        public boolean willDestroy() {
            return destroy;
        }
 
        public void setWillClose(boolean close) {
            this.close = close;
        }
 
        public void setWillDestroy(boolean destroy) {
            this.destroy = destroy;
        }
 
        public ItemStack getItem() {
            return item;
        }
    }
    
    private ItemStack setItemNameAndLore(ItemStack item, String name, String[] lore) {
        ItemMeta im = item.getItemMeta();
        im.setDisplayName(ChatColor.RESET + Main.parseColorCodes(name));
        List<String> list = new ArrayList<String>();
        for (String s : lore){
        	list.add(ChatColor.RESET + Main.parseColorCodes(s));
        }
        im.setLore(list);
        item.setItemMeta(im);
        return item;
    }
    
    public void fillEdgesWithGlass(){
    	ItemStack icon = new ItemStack(Material.STAINED_GLASS_PANE);
    	String name = "";
    	int[] position = {
    			0,
    			1,
    			2,
    			3,
    			4,
    			5,
    			6,
    			7,
    			8,
    			17,
    			26,
    			35,
    			44,
    			53,
    			53,
    			52,
    			51,
    			50,
    			49,
    			48,
    			47,
    			46,
    			45,
    			36,
    			27,
    			18,
    			9
    			};
    	
    	for (int i : position) setOption(i, icon, name);
    		
    }
}
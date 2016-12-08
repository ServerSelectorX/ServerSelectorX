package com.robinmc.serverselectorx.utils;

import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemBuilder {
	
	private ItemStack item;
	
	public ItemBuilder(Material material){
		item = new ItemStack(material);
	}
	
	public ItemBuilder(ItemStack item){
		this.item = item;
	}
	
	public ItemBuilder(String skullOwner){
		item = new ItemBuilder(Material.SKULL_ITEM).setDamage(3).setSkullOwner(skullOwner).create();
	}
	
	public ItemBuilder setAmount(int amount){
		item.setAmount(amount);
		return this;
	}
	
	public ItemBuilder setDamage(int i){
		item.setDurability((short) i);
		return this;
	}
	
	public ItemBuilder setName(String name){
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		item.setItemMeta(meta);
		return this;
	}
	
	public ItemBuilder setLore(String... lore){
		final ItemMeta meta = item.getItemMeta();
		meta.setLore(Arrays.asList(lore));
		item.setItemMeta(meta);
		return this;
	}
	
	public ItemBuilder setSkullOwner(String playerName){
		final SkullMeta meta = (SkullMeta) item.getItemMeta();
		meta.setOwner(playerName);
		item.setItemMeta(meta);
		return this;
	}
	
	public ItemBuilder setLeatherArmorColor(Color color){
		final LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
		meta.setColor(color);
		return this;
	}
	
	public ItemBuilder addEnchantment(Enchantment type, int level){
		item.addEnchantment(type, level);
		return this;
	}
	
	public ItemStack create(){
		return item;
	}
	
	public void addToInventory(Player player){
		player.getInventory().addItem(item);
	}

}

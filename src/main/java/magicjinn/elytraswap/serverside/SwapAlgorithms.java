package magicjinn.elytraswap.serverside;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Port of JJElytraSwap search/scoring/trigger rules for ServerPlayer.
 */
public final class SwapAlgorithms {
	private SwapAlgorithms() {
	}

	/**
	 * Blacklist conditions. Creative mode has full authority over inventory,
	 * causing untold problems. Players with nothing in their chest slot should not
	 * swap. Players who opt out are not eligible.
	 */
	public static boolean canOperate(ServerPlayer player) {
		return !player.isCreative()
				&& !player.isSpectator()
				&& !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
				&& !PlayerOptOut.isOptedOut(player);
	}

	/**
	 * Check if the player is airborne for elytra swapping, following vanilla elytra
	 * logic. Mirrors conditions but we cannot use the vanilla method because it
	 * requires wearing an elytra.
	 */
	public static boolean isAirborneForElytra(ServerPlayer player) {
		return !player.onGround()
				&& !player.isFallFlying()
				&& !player.isInWater()
				&& !player.hasEffect(MobEffects.LEVITATION);
	}

	public static boolean shouldWearChestplateBecauseOfGroundState(ServerPlayer player) {
		return player.onGround() || player.isInWater();
	}

	public static boolean isWearingGlider(ServerPlayer player) {
		return isGlider(player.getItemBySlot(EquipmentSlot.CHEST));
	}

	/** Worn chest item has Curse of Binding and must not be swapped off. */
	public static boolean isChestBound(ServerPlayer player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		return !chest.isEmpty() && getEnchantmentLevel(player, Enchantments.BINDING_CURSE, chest) > 0;
	}

	public static boolean isGlider(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.GLIDER);
	}

	/** Find the best elytra slot for swapping. */
	public static Integer findBestElytraSlot(ServerPlayer player) {
		List<Integer> slots = getElytraSlots(player);
		if (slots.isEmpty())
			return null;

		slots.sort(Comparator.comparingInt(slot -> getElytraStat(player, player.getInventory().getItem(slot))));
		return slots.get(slots.size() - 1);
	}

	public static Integer findBestChestplateSlot(ServerPlayer player) {
		List<Integer> slots = getChestplateSlots(player)
				.stream()
				.filter(slot -> getChestplateStat(player, player.getInventory().getItem(slot)) > 0)
				.sorted(Comparator.comparingInt(slot -> getChestplateStat(player, player.getInventory().getItem(slot))))
				.collect(Collectors.toCollection(ArrayList::new));
		Collections.reverse(slots);
		return slots.isEmpty() ? null : slots.get(0);
	}

	public static List<Integer> getElytraSlots(ServerPlayer player) {
		List<Integer> elytraSlots = new ArrayList<>();
		for (int slot : slotArray()) {
			if (isGlider(player.getInventory().getItem(slot)))
				elytraSlots.add(slot);
		}
		return elytraSlots;
	}

	public static List<Integer> getChestplateSlots(ServerPlayer player) {
		List<Integer> chestplateSlots = new ArrayList<>();
		for (int slot : slotArray()) {
			if (isSlotChestplate(player, slot))
				chestplateSlots.add(slot);
		}
		return chestplateSlots;
	}

	public static boolean isSlotChestplate(ServerPlayer player, int slotId) {
		ItemStack stack = player.getInventory().getItem(slotId);
		if (stack.isEmpty() || !stack.has(DataComponents.EQUIPPABLE))
			return false;

		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null
				&& equippable.slot() == EquipmentSlot.CHEST
				&& getEnchantmentLevel(player, Enchantments.BINDING_CURSE, stack) == 0;
	}

	/** Calculate the stat score for an elytra. */
	public static int getElytraStat(ServerPlayer player, ItemStack elytraItem) {
		return (getEnchantmentLevel(player, Enchantments.MENDING, elytraItem) * 3 + 1)
				+ getEnchantmentLevel(player, Enchantments.UNBREAKING, elytraItem);
	}

	public static int getChestplateStat(ServerPlayer player, ItemStack chestplateItem) {
		float score = 1;
		if (chestplateItem.has(DataComponents.EQUIPPABLE)) {
			Equippable equippable = chestplateItem.get(DataComponents.EQUIPPABLE);
			if (equippable != null && equippable.slot() == EquipmentSlot.CHEST) {
				var component = chestplateItem.get(DataComponents.ATTRIBUTE_MODIFIERS);
				if (component != null) {
					for (var entry : component.modifiers()) {
						if (entry.attribute() == Attributes.ARMOR)
							score += entry.modifier().amount();

						if (entry.attribute() == Attributes.ARMOR_TOUGHNESS)
							score += entry.modifier().amount();
					}
				}
				// Weights taken from JJElytraSwap: https://github.com/JumperOnJava/JJElytraSwap
				score += getEnchantmentLevel(player, Enchantments.PROTECTION, chestplateItem) * 2;
				score += getEnchantmentLevel(player, Enchantments.MENDING, chestplateItem) * 0.5;
				score += chestplateItem.has(DataComponents.CUSTOM_NAME) ? 0.25 : 0;
				score += getEnchantmentLevel(player, Enchantments.UNBREAKING, chestplateItem) * 0.24 / 3;
			}
		}
		return (int) (score * 1000);
	}

	private static int getEnchantmentLevel(ServerPlayer player, ResourceKey<Enchantment> key, ItemStack stack) {
		Registry<Enchantment> registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> holder = registry.get(key).orElse(null);
		if (holder == null)
			return 0;

		return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}

	/** Hotbar 8>0, main 35>9, then offhand 40. */
	public static int[] slotArray() {
		int[] range = new int[37];
		for (int i = 0; i < 9; i++) {
			range[i] = 8 - i;
		}
		for (int i = 9; i < 36; i++) {
			range[i] = 35 - (i - 9);
		}
		range[36] = 40;
		return range;
	}
}

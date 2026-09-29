package magicjinn.elytraswap.serverside;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

/**
 * Client-only phantom elytra overlay. Server inventory is never written here.
 */
public final class PhantomElytra {
	private static final Identifier PHANTOM_ARMOR_ID = ElytraswapServerside.id("phantom_armor");
	private static final Identifier PHANTOM_TOUGHNESS_ID = ElytraswapServerside.id("phantom_toughness");
	private static final int REFRESH_INTERVAL_TICKS = 40;

	private static final String PHANTOM_NAME_STRING = "Phantom Elytra";
	private static final String PHANTOM_LORE_STRING = "If you obtained this item, report it to the Elytraswap Serverside mod author.";

	private static final Map<UUID, PhantomState> ACTIVE = new ConcurrentHashMap<>();

	private PhantomElytra() {
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	public static void apply(ServerPlayer player) {
		// Find the best elytra slot
		Integer slot = SwapAlgorithms.findBestElytraSlot(player);
		if (slot == null) {
			clear(player);
			return;
		}

		// Get the elytra and chestplate from their slots
		ItemStack elytra = player.getInventory().getItem(slot);
		ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);

		PhantomState previous = ACTIVE.get(player.getUUID());
		boolean unchanged = previous != null
				&& previous.sourceSlot() == slot
				&& ItemStack.isSameItemSameComponents(previous.sourceElytra(), elytra)
				&& ItemStack.isSameItemSameComponents(previous.sourceChestplate(), chestplate);

		// Exit early if the state is unchanged and has not expired
		if (unchanged && player.tickCount - previous.lastSentTick() < REFRESH_INTERVAL_TICKS)
			return;

		ItemStack phantom = buildPhantom(elytra, chestplate);

		ACTIVE.put(player.getUUID(),
				new PhantomState(slot, elytra.copy(), chestplate.copy(), phantom, player.tickCount));
		sendChestEquipment(player, phantom); // Send the phantom elytra to the client
	}

	public static void clear(ServerPlayer player) {
		if (ACTIVE.remove(player.getUUID()) != null)
			sendChestEquipment(player, player.getItemBySlot(EquipmentSlot.CHEST));
	}

	public static void clearQuiet(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}

	public static void remove(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}

	/**
	 * Rewrite an outgoing equipment packet's chest slot while phantom is active.
	 */
	public static ItemStack phantomOrNull(ServerPlayer player) {
		PhantomState state = ACTIVE.get(player.getUUID());
		return state == null ? null : state.phantom().copy();
	}

	// Build the phantom elytra from the source chestplate
	private static ItemStack buildPhantom(ItemStack elytra, ItemStack chestplate) {
		ItemStack phantom = elytra.copy();

		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		ItemAttributeModifiers existing = phantom.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
				ItemAttributeModifiers.EMPTY);
		for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
			if (!entry.modifier().id().equals(PHANTOM_ARMOR_ID)
					&& !entry.modifier().id().equals(PHANTOM_TOUGHNESS_ID))
				builder.add(entry.attribute(), entry.modifier(), entry.slot());
		}

		// To preserve the illusion of the chestplate, we need to add the armor and
		// toughness attributes back to the phantom elytra, so they remain visible on
		// the clients UI
		double armor = 0;
		double toughness = 0;
		ItemAttributeModifiers chestMods = chestplate.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
				ItemAttributeModifiers.EMPTY);
		for (ItemAttributeModifiers.Entry entry : chestMods.modifiers()) {
			if (entry.attribute() == Attributes.ARMOR) {
				armor += entry.modifier().amount();
			} else if (entry.attribute() == Attributes.ARMOR_TOUGHNESS) {
				toughness += entry.modifier().amount();
			}
		}

		if (armor != 0)
			builder.add(
					Attributes.ARMOR,
					new AttributeModifier(PHANTOM_ARMOR_ID, armor, AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.CHEST);

		if (toughness != 0)
			builder.add(
					Attributes.ARMOR_TOUGHNESS,
					new AttributeModifier(PHANTOM_TOUGHNESS_ID, toughness, AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.CHEST);

		// Set attributes, name and lore
		phantom.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
		phantom.set(DataComponents.CUSTOM_NAME, Component.literal(PHANTOM_NAME_STRING));
		phantom.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		phantom.set(DataComponents.LORE, new ItemLore(List.of(
				Component.literal(PHANTOM_LORE_STRING)
						.withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC))));
		return phantom;
	}

	private static void sendChestEquipment(ServerPlayer player, ItemStack stack) {
		player.connection.send(new ClientboundSetEquipmentPacket(
				player.getId(),
				List.of(Pair.of(EquipmentSlot.CHEST, stack.copy()))));
	}

	private record PhantomState(
			int sourceSlot,
			ItemStack sourceElytra,
			ItemStack sourceChestplate,
			ItemStack phantom,
			int lastSentTick) {
	}
}

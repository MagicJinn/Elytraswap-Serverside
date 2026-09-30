package magicjinn.elytraswap.serverside;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Client-only glider. The worn chestplate is rewritten in equipment
 * packets with {@link DataComponents#GLIDER} so the client can start takeoff.
 * Server inventory is never written here.
 */
public final class PhantomElytra {
	private static final int REFRESH_INTERVAL_TICKS = 40;

	private static final Map<UUID, PhantomState> ACTIVE = new ConcurrentHashMap<>();

	private PhantomElytra() {
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	public static void apply(ServerPlayer player) {
		// Still need a real usable elytra ready for the server-side swap on takeoff.
		if (SwapAlgorithms.findBestElytraSlot(player) == null) {
			clear(player);
			return;
		}

		// Find the slot with the chestplate.
		ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
		if (chestplate.isEmpty()) {
			clear(player);
			return;
		}

		PhantomState previous = ACTIVE.get(player.getUUID());
		boolean unchanged = previous != null
				&& ItemStack.isSameItemSameComponents(previous.sourceChestplate(), chestplate);

		// Exit early if the state is unchanged and has not expired
		if (unchanged && player.tickCount - previous.lastSentTick() < REFRESH_INTERVAL_TICKS)
			return;

		ItemStack glider = buildGlider(chestplate);

		ACTIVE.put(player.getUUID(), new PhantomState(chestplate.copy(), glider, player.tickCount));
		sendChestEquipment(player, glider);
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
		return state == null ? null : state.overlay().copy();
	}

	/** Chestplate copy with GLIDER so the client treats it as glide-capable. */
	private static ItemStack buildGlider(ItemStack chestplate) {
		ItemStack overlay = chestplate.copy();
		overlay.set(DataComponents.GLIDER, Unit.INSTANCE);
		return overlay;
	}

	private static void sendChestEquipment(ServerPlayer player, ItemStack stack) {
		player.connection.send(new ClientboundSetEquipmentPacket(
				player.getId(),
				List.of(Pair.of(EquipmentSlot.CHEST, stack.copy()))));
	}

	private record PhantomState(
			ItemStack sourceChestplate,
			ItemStack overlay,
					int lastSentTick) {
	}
}

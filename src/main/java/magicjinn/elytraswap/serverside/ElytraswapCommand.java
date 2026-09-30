package magicjinn.elytraswap.serverside;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ElytraswapCommand {
	private static final String ONLY_PLAYERS_CAN = "Only players can use this command";

	private ElytraswapCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("elytraswap")
						.executes(cmandContext -> status(cmandContext.getSource()))
						.then(Commands.literal("toggle").executes(cmandContext -> toggle(cmandContext.getSource()))));
	}

	private static int toggle(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal(ONLY_PLAYERS_CAN));
			return 0;
		}

		boolean optedOut = PlayerOptOut.toggle(player);
		if (optedOut)
			PhantomElytra.clear(player);

		source.sendSuccess(() -> getStatusMessage(optedOut), false);

		return Command.SINGLE_SUCCESS;
	}

	private static int status(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal(ONLY_PLAYERS_CAN));
			return 0;
		}

		boolean optedOut = PlayerOptOut.isOptedOut(player);
		source.sendSuccess(() -> getStatusMessage(optedOut), false);
		return Command.SINGLE_SUCCESS;
	}

	private static Component getStatusMessage(boolean optedOut) {
		return Component.literal("Elytraswap is " + (optedOut ? "disabled" : "enabled") + " for you");
	}
}

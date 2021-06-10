package ml.northwestwind.survivalspectator;

import ml.northwestwind.survivalspectator.data.PositionData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;

public class SurvivalSpectator implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(CommandManager.literal("s").executes(context -> handlePlayer(context.getSource().getEntity()))));
	}

	private static int handlePlayer(Entity entity) {
		if (!(entity instanceof PlayerEntity)) return 0;
		if (entity.world.isClient) return 1;
		PositionData data = PositionData.get((ServerWorld) entity.world);
		ServerPlayerEntity player = (ServerPlayerEntity) entity;
		boolean spectating = player.isSpectator();
		if (spectating) data.toSurvival(player);
		else data.toSpectator(player);
		data.markDirty();
		return 1;
	}
}

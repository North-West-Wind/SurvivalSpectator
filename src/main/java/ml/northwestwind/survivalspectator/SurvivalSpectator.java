package ml.northwestwind.survivalspectator;

import ml.northwestwind.survivalspectator.data.PositionData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;

public class SurvivalSpectator implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(CommandManager.literal("s").executes(context -> handlePlayer(context.getSource().getEntity()))));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerPlayerEntity s : server.getPlayerManager().getPlayerList()) handlePlayer(s);
		});
	}

	private static int handlePlayer(Entity entity) {
		if (!(entity instanceof PlayerEntity)) return 0;
		if (entity.world.isClient) return 1;
		PositionData data = PositionData.get((ServerWorld) entity.world);
		PlayerEntity player = (PlayerEntity) entity;
		boolean spectating = player.isSpectator();
		if (spectating && !data.positions.containsKey(player.getUuid())) player.setGameMode(GameMode.SURVIVAL);
		else if (!spectating && !data.positions.containsKey(player.getUuid())) {
			data.positions.put(player.getUuid(), new Pair<>(player.getPos(), player.world.getRegistryKey()));
			data.markDirty();
			player.setGameMode(GameMode.SPECTATOR);
		} else if (spectating && data.positions.containsKey(player.getUuid())) {
			Vec3d pos = data.positions.get(player.getUuid()).getLeft();
			RegistryKey<World> dimension = data.positions.get(player.getUuid()).getRight();
			player.setGameMode(GameMode.SURVIVAL);
			if (!player.world.getRegistryKey().equals(dimension)) {
				ServerWorld world = ((ServerWorld) player.world).getServer().getWorld(dimension);
				if (world == null) return 0;
				LogManager.getLogger().info("Returning " + player.getName().asString() + " to " + dimension.getValue());
				player.moveToWorld(world);
			} else player.teleport(pos.x, pos.y, pos.z);
			data.positions.remove(player.getUuid());
			data.markDirty();
		} else {
			data.positions.put(player.getUuid(), new Pair<>(player.getPos(), player.world.getRegistryKey()));
			data.markDirty();
			player.setGameMode(GameMode.SPECTATOR);
		}
		return 1;
	}
}

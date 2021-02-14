package ml.northwestwind.survivalspectator;

import com.google.common.collect.Maps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.UUID;

public class SurvivalSpectator implements ModInitializer {
	private final HashMap<UUID, Vec3d> positions = Maps.newHashMap();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("s").executes(context -> {
				Entity entity = context.getSource().getEntity();
				if (!(entity instanceof PlayerEntity)) return 1;
				PlayerEntity player = (PlayerEntity) entity;
				boolean spectating = player.isSpectator();
				if (spectating && !positions.containsKey(player.getUuid())) player.setGameMode(GameMode.SURVIVAL);
				else if (!spectating && !positions.containsKey(player.getUuid())) {
					positions.put(player.getUuid(), player.getPos());
					player.setGameMode(GameMode.SPECTATOR);
				} else if (spectating && positions.containsKey(player.getUuid())) {
					Vec3d pos = positions.get(player.getUuid());
					player.teleport(pos.x, pos.y, pos.z);
					player.setGameMode(GameMode.SURVIVAL);
				} else {
					positions.put(player.getUuid(), player.getPos());
					player.setGameMode(GameMode.SPECTATOR);
				}
				return 1;
			}));
		});
	}
}

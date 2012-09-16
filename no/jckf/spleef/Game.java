package no.jckf.spleef;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;

public class Game implements Listener {
	private final Spleef plugin;
	private final Arena arena;
	private ArrayList<Player> players;
	private int last = 0;

	public Game(Spleef _plugin,Arena _arena) {
		plugin = _plugin;
		arena = _arena;

		players = new ArrayList<Player>();

		plugin.getServer().getPluginManager().registerEvents(this,plugin);

		start();
	}

	public boolean hasPlayers() {
		if (players.size() == 0) {
			return false;
		}

		return true;
	}

	public void start() {
		arena.save();

		String list = "";
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			Location l = player.getLocation();
			l.setY(l.getY() - 2);

			if (arena.contains(l)) {
				players.add(player);
				list += player.getDisplayName() + ", ";
			}
		}

		broadcast(ChatColor.GREEN + "The game has started! Players: " + ChatColor.WHITE + list.substring(0,Math.max(0,list.length() - 2)));
	}

	public void stop() {
		String list = "";
		for (Player player : players) {
			list += player.getDisplayName() + ", ";
		}

		broadcast(ChatColor.RED + "The game was stopped! Remaining players: " + ChatColor.WHITE + list.substring(0,Math.max(0,list.length() - 2)));

		arena.restore();
	}

	public void broadcast(String msg) {
		for (Player player : players) {
			player.sendMessage(msg);
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (!players.contains(event.getPlayer())) {
			return;
		}

		Block block = event.getClickedBlock();

		if (block == null) {
			return;
		}

		if (!arena.contains(block.getLocation())) {
			return;
		}

		block.setType(Material.AIR);
	}

	// TODO: PlayerMoveEvent is very resource heavy. Find a better solution!
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		// Ugly hack to avoid a lot of work.
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (last >= now) {
			return;
		}
		last = now;

		// Clone to avoid concurrent modification.
		for (Player p : (ArrayList<Player>)players.clone()) {
			Location l = p.getLocation();
			l.setY(l.getY() + 3);

			if (arena.contains(l)) {
				if (players.size() > 1) {
					players.remove(p);
					p.sendMessage(ChatColor.RED + "You lost!");
					broadcast(ChatColor.GREEN + p.getDisplayName() + ChatColor.GREEN + " is out!");
				}
			}
			if (players.size() == 1 && players.contains(p)) {
				plugin.getServer().broadcastMessage(ChatColor.GREEN + p.getDisplayName() + ChatColor.GREEN + " just won a round of spleef!");
				players.remove(p);

				arena.restore();
			}
		}
	}

	@EventHandler
	public void onDisconnect(PlayerQuitEvent event) {
		players.remove(event.getPlayer());
	}
}
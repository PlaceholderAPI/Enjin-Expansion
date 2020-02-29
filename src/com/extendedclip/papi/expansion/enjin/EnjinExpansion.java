package com.extendedclip.papi.expansion.enjin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Cleanable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.enjin.core.EnjinServices;
import com.enjin.rpc.mappings.mappings.general.RPCData;
import com.enjin.rpc.mappings.services.PointService;

public class EnjinExpansion extends PlaceholderExpansion implements Cleanable, Taskable, Cacheable, Configurable {

	private final Map<String, Integer> points = new ConcurrentHashMap<String, Integer>();
	
	private BukkitTask task;
	
	private PointService service;
	
	private int fetchInterval;

	@Override
	public String getAuthor() {
		return "clip";
	}

	@Override
	public String getIdentifier() {
		return "enjin";
	}

	@Override
	public String getPlugin() {
		return "EnjinMinecraftPlugin";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}
	
	@Override
	public String onPlaceholderRequest(Player p, String identifier) {

		if (p == null) {
			return "";
		}

		if (identifier.equals("points")) {

			if (points.containsKey(p.getName())) {
				return String.valueOf(points.get(p.getName()));
			} else {
				points.put(p.getName(), 0);
				return "0";
			}

		}

		return null;
	}
	
	@Override
	public boolean canRegister() {
		return Bukkit.getPluginManager().getPlugin(getPlugin()) != null;
	}
	
	@Override
	public boolean register() {
		
		try {
			service = (PointService)EnjinServices.getService(PointService.class);
			
		} catch (Exception ex) {
			return false;
		}
		
		int interval = getInt("check_interval", 60);
		
		if (interval > 0) {
			fetchInterval = interval;
		} else {
			fetchInterval = 60;
		}
		
		return super.register();
	}
	
	@Override
	public Map<String, Object> getDefaults() {
		final Map<String, Object> defaults = new HashMap<String, Object>();
		defaults.put("check_interval", 30);
		return defaults;
	}

	@Override
	public void start() {
		
		task = new BukkitRunnable() {

			@Override
			public void run() { 
				
				Set<String> names = new HashSet<String>(points.keySet());
				
				for (String name : names) {
					
					RPCData<Integer> data = service.get(name);
					
					if (data == null) {
						continue;
					}
					
					if (data.getError() == null) {
						points.put(name, data.getResult());
					}
				}
			}
			
		}.runTaskTimerAsynchronously(getPlaceholderAPI(), 100L, 20L*fetchInterval);
	}

	@Override
	public void stop() {
		if (task != null) {
			try {
				task.cancel();
			} catch (Exception ex) {	
			}
			task = null;
		}
	}

	@Override
	public void clear() {
		if (points != null) {
			points.clear();
		}
	}

	@Override
	public void cleanup(Player p) {
		points.remove(p.getName());
	}
}


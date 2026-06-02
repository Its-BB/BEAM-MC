package org.bibi.beam;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class Beam extends JavaPlugin {

    private final Map<String, BeamData> activeBeams = new HashMap<>();
    private final Map<String, BukkitTask> beamTasks = new HashMap<>();
    private final Map<String, PositionData> savedPositions = new HashMap<>();
    private FileConfiguration config;
    private File configJsonFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        configJsonFile = new File(getDataFolder(), "config.json");

        if (!configJsonFile.exists()) {
            try {
                configJsonFile.createNewFile();
                try (FileWriter writer = new FileWriter(configJsonFile)) {
                    writer.write("{ \"beams\": [], \"positions\": [] }");
                }
            } catch (IOException e) {
                getLogger().severe("Could not create config.json: " + e.getMessage());
            }
        }

        getCommand("sbeam").setExecutor(this);
        loadBeamsFromJson();
        loadPositionsFromJson();
        startDamageTask();
        getLogger().info("SBeam plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : beamTasks.values()) {
            task.cancel();
        }
        beamTasks.clear();
        saveBeamsToJson();
        savePositionsToJson();
        getLogger().info("SBeam plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("sbeam")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("sbeam.use")) {
                player.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }

            if (args.length < 1) {
                player.sendMessage("§cUsage: /sbeam <name> or /sbeam remove <name> or /sbeam pos <x y z> <name> or /sbeam run all");
                return true;
            }

            // Handle the /sbeam pos command
            if (args[0].equalsIgnoreCase("pos")) {
                if (args.length < 5) {
                    player.sendMessage("§cUsage: /sbeam pos <x> <y> <z> <name>");
                    return true;
                }

                try {
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    String posName = args[4];

                    if (savedPositions.containsKey(posName)) {
                        player.sendMessage("§cA position with this name already exists!");
                        return true;
                    }

                    savePosition(player, posName, x, y, z);
                    player.sendMessage("§aPosition §e" + posName + " §ahas been saved at §e" + x + ", " + y + ", " + z);
                    return true;
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid coordinates! Use numbers for x, y, and z.");
                    return true;
                }
            }

            // Handle the /sbeam run all command
            if (args[0].equalsIgnoreCase("run") && args.length > 1 && args[1].equalsIgnoreCase("all")) {
                if (savedPositions.isEmpty()) {
                    player.sendMessage("§cNo positions have been saved yet!");
                    return true;
                }

                player.sendMessage("§a§lInitializing all beams with 1.5 second delay between each...");
                runAllBeamsWithDelay(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("remove")) {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /sbeam remove <name>");
                    return true;
                }

                String beamName = args[1];
                if (!activeBeams.containsKey(beamName)) {
                    player.sendMessage("§cNo beam with the name '" + beamName + "' exists!");
                    return true;
                }

                removeBeam(beamName);
                player.sendMessage("§aBeam '" + beamName + "' has been removed!");
                return true;
            }

            String beamName = args[0];
            if (activeBeams.containsKey(beamName)) {
                player.sendMessage("§cA beam with this name already exists!");
                return true;
            }

            // Play custom audio2 sound for all players at their exact location with max volume and no distance limit
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                // Using player's exact location, volume 2.0 (very loud), and pitch 1.0 (normal)
                // The 2.0 volume and playing directly at player location ensures they can hear it
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "playsound custom:audio2 master " + onlinePlayer.getName() + " " +
                                onlinePlayer.getLocation().getX() + " " +
                                onlinePlayer.getLocation().getY() + " " +
                                onlinePlayer.getLocation().getZ() + " " +
                                "2.0 1.0");
            }

            player.sendMessage("§aBeam initialization started! Stand back!");

            // Create beam after 1 second delay following the sound
            new BukkitRunnable() {
                @Override
                public void run() {
                    createBeam(player, beamName);
                }
            }.runTaskLater(this, 20L); // 20 ticks = 1 second

            return true;
        }
        return false;
    }

    private void savePosition(Player player, String posName, double x, double y, double z) {
        PositionData posData = new PositionData(
                posName,
                player.getWorld().getName(),
                x,
                y,
                z
        );
        savedPositions.put(posName, posData);
        savePositionsToJson();
    }

    private void runAllBeamsWithDelay(Player player) {
        List<PositionData> positions = new ArrayList<>(savedPositions.values());

        // Start running beams with delay
        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= positions.size()) {
                    cancel();
                    return;
                }

                PositionData pos = positions.get(index);
                String beamName = pos.getName();

                // Skip if beam with this name already exists
                if (activeBeams.containsKey(beamName)) {
                    player.sendMessage("§cSkipping beam '" + beamName + "' as it already exists!");
                    index++;
                    return;
                }

                // Play audio effect for all players
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "playsound custom:audio2 master " + onlinePlayer.getName() + " " +
                                    onlinePlayer.getLocation().getX() + " " +
                                    onlinePlayer.getLocation().getY() + " " +
                                    onlinePlayer.getLocation().getZ() + " " +
                                    "2.0 1.0");
                }

                player.sendMessage("§aInitializing beam: §e" + beamName);

                // Create beam after a short delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        World world = Bukkit.getWorld(pos.getWorldName());
                        if (world != null) {
                            Location beamLoc = new Location(world, pos.getX(), pos.getY(), pos.getZ());
                            createBeamAtLocation(beamName, beamLoc);
                            player.sendMessage("§a§lBeam §e" + beamName + " §a§lhas been created!");
                        } else {
                            player.sendMessage("§cWorld not found for beam: " + beamName);
                        }
                    }
                }.runTaskLater(Beam.this, 20L); // 1 second delay

                index++;
            }
        }.runTaskTimer(this, 0L, 30L); // 1.5 second delay (30 ticks)
    }

    private void createBeam(Player player, String beamName) {
        Location playerLoc = player.getLocation();
        Location beamLoc = playerLoc.clone();
        beamLoc.setY(beamLoc.getY() - 4);

        createBeamAtLocation(beamName, beamLoc);
        player.sendMessage("§a§lBeam §e" + beamName + " §a§lhas been created!");
    }

    private void createBeamAtLocation(String beamName, Location beamLoc) {
        World world = beamLoc.getWorld();

        BeamData beamData = new BeamData(
                beamName,
                world.getName(),
                beamLoc.getX(),
                beamLoc.getY(),
                beamLoc.getZ()
        );
        activeBeams.put(beamName, beamData);

        transformSurroundingBlocks(beamLoc, 10);
        createBeamEffect(beamName, beamLoc);
        saveBeamsToJson();
    }

    private void removeBeam(String beamName) {
        if (beamTasks.containsKey(beamName)) {
            beamTasks.get(beamName).cancel();
            beamTasks.remove(beamName);
        }
        activeBeams.remove(beamName);
        saveBeamsToJson();
    }

    private void transformSurroundingBlocks(Location center, int radius) {
        World world = center.getWorld();
        Random random = new Random();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    if (distance <= radius) {
                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = blockLoc.getBlock();
                        if (!block.getType().isAir() && block.getType().isSolid()) {
                            if (random.nextBoolean()) {
                                block.setType(Material.SCULK);
                            } else {
                                block.setType(Material.OBSIDIAN);
                            }
                        }
                    }
                }
            }
        }
    }

    private void createBeamEffect(String beamName, Location location) {
        World world = location.getWorld();
        final int beamHeight = 300;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Only use SOUL_FIRE_FLAME particles for the beam
                for (int y = 0; y < beamHeight; y += 1) {
                    Location particleLoc = location.clone().add(0, y, 0);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 10, 0.2, 0, 0.2, 0.01);
                }

                // Add base effect using only SOUL_FIRE_FLAME
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 30, 0.5, 0.5, 0.5, 0.05);
            }
        }.runTaskTimer(this, 0L, 3L);

        beamTasks.put(beamName, task);
    }

    private void startDamageTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (BeamData beam : activeBeams.values()) {
                    World world = Bukkit.getWorld(beam.getWorldName());
                    if (world == null) continue;

                    Location beamLoc = new Location(world, beam.getX(), beam.getY(), beam.getZ());
                    for (Player player : world.getPlayers()) {
                        double distance = player.getLocation().distance(beamLoc);
                        if (distance <= 5) {
                            player.damage(2.0);
                            player.sendMessage("§c§lYou're being damaged by the beam!");
                            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN, 1.0f, 1.0f);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @SuppressWarnings("unchecked")
    private void loadBeamsFromJson() {
        try {
            if (configJsonFile.length() == 0) {
                try (FileWriter writer = new FileWriter(configJsonFile)) {
                    writer.write("{ \"beams\": [], \"positions\": [] }");
                }
                return;
            }

            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(configJsonFile)) {
                Object obj = parser.parse(reader);
                JSONObject jsonObject = (JSONObject) obj;
                JSONArray beamsArray = (JSONArray) jsonObject.get("beams");
                if (beamsArray == null) {
                    beamsArray = new JSONArray();
                    jsonObject.put("beams", beamsArray);
                    try (FileWriter writer = new FileWriter(configJsonFile)) {
                        writer.write(jsonObject.toJSONString());
                    }
                    return;
                }

                for (Object beamObj : beamsArray) {
                    JSONObject beamJson = (JSONObject) beamObj;
                    String name = (String) beamJson.get("name");
                    String worldName = (String) beamJson.get("world");
                    double x = ((Number) beamJson.get("x")).doubleValue();
                    double y = ((Number) beamJson.get("y")).doubleValue();
                    double z = ((Number) beamJson.get("z")).doubleValue();
                    BeamData beam = new BeamData(name, worldName, x, y, z);
                    activeBeams.put(name, beam);
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location beamLoc = new Location(world, x, y, z);
                        createBeamEffect(name, beamLoc);
                    }
                }
                getLogger().info("Loaded " + activeBeams.size() + " beams from config.json");
            }
        } catch (IOException | ParseException e) {
            getLogger().severe("Error loading beams from config.json: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPositionsFromJson() {
        try {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(configJsonFile)) {
                Object obj = parser.parse(reader);
                JSONObject jsonObject = (JSONObject) obj;
                JSONArray positionsArray = (JSONArray) jsonObject.get("positions");
                if (positionsArray == null) {
                    positionsArray = new JSONArray();
                    jsonObject.put("positions", positionsArray);
                    try (FileWriter writer = new FileWriter(configJsonFile)) {
                        writer.write(jsonObject.toJSONString());
                    }
                    return;
                }

                for (Object posObj : positionsArray) {
                    JSONObject posJson = (JSONObject) posObj;
                    String name = (String) posJson.get("name");
                    String worldName = (String) posJson.get("world");
                    double x = ((Number) posJson.get("x")).doubleValue();
                    double y = ((Number) posJson.get("y")).doubleValue();
                    double z = ((Number) posJson.get("z")).doubleValue();
                    PositionData position = new PositionData(name, worldName, x, y, z);
                    savedPositions.put(name, position);
                }
                getLogger().info("Loaded " + savedPositions.size() + " positions from config.json");
            }
        } catch (IOException | ParseException e) {
            getLogger().severe("Error loading positions from config.json: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void saveBeamsToJson() {
        if (configJsonFile == null) {
            getLogger().severe("Cannot save beams: config file is null");
            return;
        }

        try {
            JSONObject root = readOrCreateJsonRoot();
            JSONArray beamsArray = new JSONArray();
            for (Map.Entry<String, BeamData> entry : activeBeams.entrySet()) {
                BeamData beamData = entry.getValue();
                JSONObject beamJson = new JSONObject();
                beamJson.put("name", beamData.getName());
                beamJson.put("world", beamData.getWorldName());
                beamJson.put("x", beamData.getX());
                beamJson.put("y", beamData.getY());
                beamJson.put("z", beamData.getZ());
                beamsArray.add(beamJson);
            }
            root.put("beams", beamsArray);

            // Keep positions array intact
            if (!root.containsKey("positions")) {
                root.put("positions", new JSONArray());
            }

            try (FileWriter writer = new FileWriter(configJsonFile)) {
                writer.write(root.toJSONString());
            }
        } catch (IOException e) {
            getLogger().severe("Error saving beams to config.json: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void savePositionsToJson() {
        if (configJsonFile == null) {
            getLogger().severe("Cannot save positions: config file is null");
            return;
        }

        try {
            JSONObject root = readOrCreateJsonRoot();
            JSONArray positionsArray = new JSONArray();
            for (Map.Entry<String, PositionData> entry : savedPositions.entrySet()) {
                PositionData posData = entry.getValue();
                JSONObject posJson = new JSONObject();
                posJson.put("name", posData.getName());
                posJson.put("world", posData.getWorldName());
                posJson.put("x", posData.getX());
                posJson.put("y", posData.getY());
                posJson.put("z", posData.getZ());
                positionsArray.add(posJson);
            }
            root.put("positions", positionsArray);

            // Keep beams array intact
            if (!root.containsKey("beams")) {
                root.put("beams", new JSONArray());
            }

            try (FileWriter writer = new FileWriter(configJsonFile)) {
                writer.write(root.toJSONString());
            }
        } catch (IOException e) {
            getLogger().severe("Error saving positions to config.json: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject readOrCreateJsonRoot() {
        JSONObject root = new JSONObject();

        if (!configJsonFile.exists()) {
            try {
                configJsonFile.createNewFile();
                root.put("beams", new JSONArray());
                root.put("positions", new JSONArray());
                try (FileWriter writer = new FileWriter(configJsonFile)) {
                    writer.write(root.toJSONString());
                }
            } catch (IOException e) {
                getLogger().severe("Could not create config.json: " + e.getMessage());
            }
            return root;
        }

        try {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(configJsonFile)) {
                Object obj = parser.parse(reader);
                return (JSONObject) obj;
            }
        } catch (IOException | ParseException e) {
            getLogger().severe("Error reading config.json: " + e.getMessage());
            return root;
        }
    }

    private static class BeamData {
        private final String name;
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;

        public BeamData(String name, String worldName, double x, double y, double z) {
            this.name = name;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getName() {
            return name;
        }

        public String getWorldName() {
            return worldName;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }

    private static class PositionData {
        private final String name;
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;

        public PositionData(String name, String worldName, double x, double y, double z) {
            this.name = name;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getName() {
            return name;
        }

        public String getWorldName() {
            return worldName;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }
}
package de.menu.friendsysten;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class FriendSystem extends JavaPlugin implements Listener {

    private HashMap<UUID, List<String>> friendData = new HashMap<>();
    private HashMap<UUID, List<String>> friendRequests = new HashMap<>();

    @Override
    public void onEnable() {
        loadFriendData();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveFriendData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern verwendet werden.");
            return false;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("friend")) {
            if (args.length == 0) {
                showUsage(player);
                return true;
            }

            switch (args[0]) {
                case "add":
                    if (args.length != 2) {
                        showUsage(player);
                        return true;
                    }
                    addFriend(player, args[1]);
                    return true;
                case "remove":
                    if (args.length != 2) {
                        showUsage(player);
                        return true;
                    }
                    removeFriend(player, args[1]);
                    return true;
                case "accept":
                    if (args.length != 2) {
                        showUsage(player);
                        return true;
                    }
                    acceptFriendRequest(player, args[1]);
                    return true;
                case "deny":
                    if (args.length != 2) {
                        showUsage(player);
                        return true;
                    }
                    denyFriendRequest(player, args[1]);
                    return true;
                case "list":
                    displayFriendList(player);
                    return true;
                default:
                    showUsage(player);
                    return true;
            }
        }

        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Nachricht mit Freundschaftsanfragen senden
        sendFriendRequests(player);

        // Nachricht mit Online-Freunden senden
        sendOnlineFriends(player);

        // Sicherstellen, dass der Spieler in der friendData-Map vorhanden ist
        if (!friendData.containsKey(playerUUID)) {
            friendData.put(playerUUID, new ArrayList<>());
        }
    }

    private void sendFriendRequests(Player player) {
        List<String> requests = friendRequests.get(player.getUniqueId());
        if (requests != null && !requests.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Offene Freundschaftsanfragen:");
            for (String request : requests) {
                player.sendMessage(ChatColor.YELLOW + "- " + request);
            }
        }
    }

    private void sendOnlineFriends(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<String> playerFriends = friendData.getOrDefault(playerUUID, new ArrayList<>());
        player.sendMessage(ChatColor.YELLOW + "Deine Freunde, die online sind:");
        for (String friendName : playerFriends) {
            Player friend = Bukkit.getPlayerExact(friendName);
            if (friend != null && friend.isOnline()) {
                player.sendMessage(ChatColor.YELLOW + "- " + friendName);
            }
        }
    }


    private void showUsage(Player player) {
        player.sendMessage(ChatColor.GREEN + "Verwendung:");
        player.sendMessage(ChatColor.YELLOW + "/friend add <Spielername>");
        player.sendMessage(ChatColor.YELLOW + "/friend remove <Spielername>");
        player.sendMessage(ChatColor.YELLOW + "/friend accept <Spielername>");
        player.sendMessage(ChatColor.YELLOW + "/friend deny <Spielername>");
        player.sendMessage(ChatColor.YELLOW + "/friend list");
    }

    private void addFriend(Player player, String friendName) {
        Player friendPlayer = Bukkit.getPlayer(friendName);
        if (friendPlayer != null) {
            UUID friendUUID = friendPlayer.getUniqueId();
            List<String> playerFriends = friendData.get(player.getUniqueId());
            List<String> friendRequests = friendData.getOrDefault(friendUUID, new ArrayList<>());

            if (!playerFriends.contains(friendName)) {
                if (!friendRequests.contains(player.getName())) {
                    friendRequests.add(player.getName());
                    friendData.put(friendUUID, friendRequests);
                    player.sendMessage(ChatColor.GREEN + "Du hast " + friendName + " eine Freundschaftsanfrage gesendet.");
                    friendPlayer.sendMessage(ChatColor.GREEN + player.getName() + " hat dir eine Freundschaftsanfrage gesendet. Verwende '/friend accept " + player.getName() + "' um anzunehmen.");
                } else {
                    player.sendMessage(ChatColor.RED + "Du hast bereits eine ausstehende Freundschaftsanfrage an " + friendName + ".");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Du bist bereits mit " + friendName + " befreundet.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Der Spieler " + friendName + " ist nicht online.");
        }
    }

    private void removeFriend(Player player, String friendName) {
        Player friendPlayer = Bukkit.getPlayer(friendName);
        if (friendPlayer != null) {
            UUID friendUUID = friendPlayer.getUniqueId();
            List<String> playerFriends = friendData.get(player.getUniqueId());
            if (playerFriends.contains(friendName)) {
                playerFriends.remove(friendName);
                player.sendMessage(ChatColor.GREEN + "Du hast " + friendName + " erfolgreich aus deiner Freundesliste entfernt.");
            } else {
                player.sendMessage(ChatColor.RED + "Du bist nicht mit" + friendName + " befreundet.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Der Spieler " + friendName + " ist nicht online.");
        }
    }

    private void acceptFriendRequest(Player player, String senderName) {
        Player sender = Bukkit.getPlayer(senderName);
        if (sender != null) {
            UUID playerUUID = player.getUniqueId();
            UUID senderUUID = sender.getUniqueId();

            List<String> playerFriends = friendData.getOrDefault(playerUUID, new ArrayList<>());
            List<String> senderRequests = friendRequests.getOrDefault(senderUUID, new ArrayList<>());

            if (senderRequests.contains(player.getName())) {
                if (!playerFriends.contains(senderName)) {
                    playerFriends.add(senderName);
                    senderRequests.remove(player.getName());

                    friendData.put(playerUUID, playerFriends);
                    friendRequests.put(senderUUID, senderRequests);

                    player.sendMessage(ChatColor.GREEN + "Du hast die Freundschaftsanfrage von " + senderName + " angenommen.");
                    sender.sendMessage(ChatColor.GREEN + player.getName() + " hat deine Freundschaftsanfrage angenommen.");
                } else {
                    player.sendMessage(ChatColor.RED + "Du bist bereits mit " + senderName + " befreundet.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Du hast keine ausstehende Freundschaftsanfrage von " + senderName + ".");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Der Spieler " + senderName + " ist nicht online.");
        }
    }



    private void denyFriendRequest(Player player, String senderName) {
        Player sender = Bukkit.getPlayer(senderName);
        if (sender != null) {
            List<String> senderRequests = friendData.getOrDefault(sender.getUniqueId(), new ArrayList<>());
            if (senderRequests.contains(player.getName())) {
                senderRequests.remove(player.getName());
                player.sendMessage(ChatColor.RED + "Du hast die Freundschaftsanfrage von " + senderName + " abgelehnt.");
                sender.sendMessage(ChatColor.RED + player.getName() + " hat deine Freundschaftsanfrage abgelehnt.");
            } else {
                player.sendMessage(ChatColor.RED + "Du hast keine ausstehende Freundschaftsanfrage von " + senderName + ".");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Der Spieler " + senderName + " ist nicht online.");
        }
    }

    private void displayFriendList(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<String> playerFriends = friendData.getOrDefault(playerUUID, new ArrayList<>());

        if (playerFriends.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Du hast keine Freunde.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Deine Freunde:");

            for (String friendName : playerFriends) {
                player.sendMessage(ChatColor.YELLOW + "- " + friendName);
            }
        }
    }

    private void loadFriendData() {
        FileConfiguration config = getConfig();
        if (config.contains("friendData")) {
            ConfigurationSection friendSection = config.getConfigurationSection("friendData");
            for (String playerUUID : friendSection.getKeys(false)) {
                List<String> friends = friendSection.getStringList(playerUUID);
                friendData.put(UUID.fromString(playerUUID), friends);
            }
        }
    }

    private void saveFriendData() {
        FileConfiguration config = getConfig();
        ConfigurationSection friendSection = config.createSection("friendData");
        for (UUID playerUUID : friendData.keySet()) {
            friendSection.set(playerUUID.toString(), friendData.get(playerUUID));
        }
        saveConfig();
    }
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.roles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.anjocaido.groupmanager.*;
import org.anjocaido.groupmanager.data.*;
import org.anjocaido.groupmanager.dataholder.*;
import org.anjocaido.groupmanager.utils.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

/**
 *
 * @author gabrielcouto
 */
public class Roles extends JavaPlugin {

    GroupManager gm;
    File configFile;
    File historyFile;
    Configuration config;
    Configuration history;
    public static final String VARKEY_CATEGORY = "roles-category";
    public static final String VARKEY_REQUIREMENT = "roles-requirement";

    @Override
    public void onDisable() {
        if (config != null) {
            config.save();
        }
        if (history != null) {
            history.save();
        }
        System.out.println(this.getDescription().getName() + " version " + this.getDescription().getVersion() + " was disabled!");
    }

    @Override
    public void onEnable() {
        Plugin p = this.getServer().getPluginManager().getPlugin("GroupManager");
        if (p != null) {           
            if (!this.getServer().getPluginManager().isPluginEnabled(p)) {
                this.getServer().getPluginManager().enablePlugin(p);
            }
            try {
                p.getClass().getClassLoader().loadClass("org.anjocaido.groupmanager.utils.Tasks");
                p.getClass().getClassLoader().loadClass("org.anjocaido.groupmanager.data.Group");
                p.getClass().getClassLoader().loadClass("org.anjocaido.groupmanager.data.User");
                p.getClass().getClassLoader().loadClass("org.anjocaido.groupmanager.dataholder.WorldDataHolder");
                p.getClass().getClassLoader().loadClass("org.anjocaido.groupmanager.GroupManager");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Roles.class.getName()).log(Level.SEVERE, null, ex);
            }
            gm = (GroupManager) p;
        } else {
            System.out.println("Roles plugin could not load the GroupManager data!");
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        configFile = new File(getDataFolder(), "config.yml");
        historyFile = new File(getDataFolder(), "history.yml");
        firstTimeCheck();
        config = new Configuration(configFile);
        history = new Configuration(historyFile);
        config.load();
        history.load();
        System.out.println(this.getDescription().getName() + " version " + this.getDescription().getVersion() + " was enabled!");
    }

    private void firstTimeCheck() {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }
        if (!configFile.exists()) {
            //System.out.println("Criando config...");
            InputStream resourceAsStream = this.getClassLoader().getResourceAsStream("config.yml");
            try {
                Tasks.copy(resourceAsStream, configFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (!historyFile.exists()) {
            //System.out.println("Criando history...");
            InputStream resourceAsStream = this.getClassLoader().getResourceAsStream("history.yml");
            try {
                Tasks.copy(resourceAsStream, historyFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Called when a command registered by this plugin is received.
     */
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            WorldDataHolder worldData = gm.getWorldsHolder().getWorldData(player);
            String world = worldData.getName();
            commandLabel = cmd.getName();
            if (worldData.getPermissionsHandler().permission(player, "roles." + commandLabel)) {
                if (commandLabel.equalsIgnoreCase("joinrole")) {
                    if (args.length == 0) {
                        sender.sendMessage(ChatColor.YELLOW + "Role categories available:");
                        sender.sendMessage(ChatColor.GREEN + Tasks.getStringListInString(getCategoryList(world)));
                        return true;
                    } else if (args.length == 1) {
                        String category = args[0].toLowerCase();
                        if (!getCategoryList(world).contains(category)) {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, I don't know that category name!");
                            return false;
                        }
                        int count = getCountUserCategory(player, category);
                        int limit = getLimit(world, category);
                        sender.sendMessage(ChatColor.YELLOW + "You are using " + count + "/" + limit + " roles for this category.");
                        ArrayList<String> joinable = getGroupsJoinableOfCategory(player, category);
                        if (joinable.isEmpty()) {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, no roles in this category are available for you right now.");
                            return false;
                        }
                        sender.sendMessage(ChatColor.YELLOW + "The following roles are available for you to join:");
                        sender.sendMessage(ChatColor.GREEN + Tasks.getStringListInString(joinable));
                        return true;
                    } else if (args.length == 2) {
                        Group g = worldData.getGroup(args[1]);
                        if (g != null) {
                            if (canUserJoinGroup(player, g.getName())) {
                                if (getMainCategory(world).equalsIgnoreCase(getGroupCategory(g))) {
                                    worldData.getUser(player.getName()).setGroup(g);
                                } else {
                                    worldData.getUser(player.getName()).addSubGroup(g);
                                }
                                history.setProperty((world.toLowerCase() + "." + player.getName().toLowerCase() + "." + g.getName().toLowerCase() + ".lastjoin"), System.currentTimeMillis());
                                sender.sendMessage(ChatColor.YELLOW + "Congratulations, you joined the role " + g.getName() + " for category " + getGroupCategory(g));
                                return true;
                            } else {
                                sender.sendMessage(ChatColor.RED + "You can't join that role!");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Role not found!");
                        }
                        return false;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Review your arguments.");
                        return false;
                    }
                } else if (commandLabel.equalsIgnoreCase("leaverole")) {
                    if (args.length == 0) {
                        sender.sendMessage(ChatColor.YELLOW + "Role categories available:");
                        sender.sendMessage(ChatColor.GREEN + Tasks.getStringListInString(getCategoryList(world)));
                        return true;
                    } else if (args.length == 1) {
                        String category = args[0].toLowerCase();
                        if (!getCategoryList(world).contains(category)) {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, I don't know that category name!");
                            return false;
                        }
                        int count = getCountUserCategory(player, category);
                        int limit = getLimit(world, category);
                        sender.sendMessage(ChatColor.YELLOW + "You are using " + count + "/" + limit + " roles for this category.");
                        ArrayList<String> inRoles = getGroupsLeavableOfCategory(player, category);
                        if (inRoles.isEmpty()) {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, you can't leave any role in this category.");
                            //sender.sendMessage(ChatColor.LIGHT_PURPLE + "You might need to wait some hours to leave it.");
                            return false;
                        }
                        sender.sendMessage(ChatColor.YELLOW + "The following roles are available for you to leave:");
                        sender.sendMessage(ChatColor.GREEN + Tasks.getStringListInString(inRoles));
                        return true;
                    } else if (args.length == 2) {
                        Group g = worldData.getGroup(args[1]);
                        if (g != null) {
                            if (canUserLeaveGroup(player, g.getName())) {
                                if (getMainCategory(world).equalsIgnoreCase(getGroupCategory(g))) {
                                    Group newGroup = worldData.getGroup(getDefaultGroup(world));
                                    if (newGroup != null) {
                                        worldData.getUser(player.getName()).setGroup(newGroup);
                                    } else {
                                        worldData.getUser(player.getName()).setGroup(worldData.getDefaultGroup());
                                    }
                                } else {
                                    worldData.getUser(player.getName()).removeSubGroup(g);
                                }
                                boolean hasRemoved = true;
                                while (hasRemoved) {
                                    hasRemoved = false;
                                    for (String role : getUserRoles(player)) {
                                        if (!doFulfillRequirements(player, role)) {
                                            worldData.getUser(player.getName()).removeSubGroup(worldData.getGroup(role));
                                            hasRemoved = true;
                                        }
                                    }
                                }
                                history.setProperty((world.toLowerCase() + "." + player.getName().toLowerCase() + "." + g.getName().toLowerCase() + ".lastjoin"), 0);
                                sender.sendMessage(ChatColor.YELLOW + "Congratulations, you leaved the role " + g.getName() + " for category " + getGroupCategory(g));
                                return true;
                            } else {
                                sender.sendMessage(ChatColor.RED + "You can't leave that role!");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Role not found!");
                        }
                        return false;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Review your arguments.");
                        return false;
                    }
                } else if (commandLabel.equalsIgnoreCase("myroles")) {
                    if (args.length == 0) {
                        sender.sendMessage(ChatColor.YELLOW + "Role categories available:");
                        sender.sendMessage(ChatColor.GREEN + Tasks.getStringListInString(getCategoryList(world)));
                        return true;
                    } else if (args.length == 1) {
                        String category = args[0].toLowerCase();
                        if (!getCategoryList(world).contains(category)) {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, I don't know that category name!");
                            return false;
                        }
                        int count = getCountUserCategory(player, category);
                        int limit = getLimit(world, category);
                        sender.sendMessage(ChatColor.YELLOW + "You are using " + count + "/" + limit + " roles for this category.");
                        ArrayList<String> inRoles = getUserRolesInCategory(player, category);
                        if (inRoles.isEmpty()) {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, you didn't join any role in this category.");
                            //sender.sendMessage(ChatColor.LIGHT_PURPLE + "You might need to wait some hours to leave it.");
                            return false;
                        }
                        sender.sendMessage(ChatColor.YELLOW + "List of roles for this category:");
                        sender.sendMessage(ChatColor.GREEN + Tasks.getStringListInString(inRoles));
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Review your arguments.");
                        return false;
                    }
                } else if (commandLabel.equalsIgnoreCase("whoroles")) {
                    if (args.length == 1) {
                        sender.sendMessage(ChatColor.YELLOW + "Role categories available:");
                        sender.sendMessage(ChatColor.GREEN + Tasks.getStringListInString(getCategoryList(world)));
                        return true;
                    } else if (args.length == 2) {
                        List<Player> matchPlayer = this.getServer().matchPlayer(args[0]);
                        if (matchPlayer.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Couldn't find that player.");
                            return false;
                        }
                        player = matchPlayer.get(0);
                        String category = args[1].toLowerCase();
                        if (!getCategoryList(world).contains(category)) {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, I don't know that category name!");
                            return false;
                        }
                        int count = getCountUserCategory(player, category);
                        int limit = getLimit(world, category);
                        sender.sendMessage(ChatColor.YELLOW + "You are using " + count + "/" + limit + " roles for this category.");
                        ArrayList<String> inRoles = getUserRolesInCategory(player, category);
                        if (inRoles.isEmpty()) {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Sorry, you didn't join any role in this category.");
                            //sender.sendMessage(ChatColor.LIGHT_PURPLE + "You might need to wait some hours to leave it.");
                            return false;
                        }
                        sender.sendMessage(ChatColor.YELLOW + "List of roles for this category:");
                        sender.sendMessage(ChatColor.GREEN + Tasks.getStringListInString(inRoles));
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Review your arguments.");
                        return false;
                    }
                } else if (commandLabel.equalsIgnoreCase("roles")) {
                    config.load();
                    sender.sendMessage(ChatColor.YELLOW + "Roles config.yml reloaded!");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have permission to access that command.");
                return true;
            }
        }

        return false; // default implementation:  do nothing!
    }

    public List<String> getCategoryList(String world) {
        return config.getKeys(world + ".categories");
    }

    public String getMainCategory(String world) {
        return config.getString(world.toLowerCase() + ".setting.maincategory");
    }

    public String getDefaultGroup(String world) {
        return config.getString(world.toLowerCase() + ".setting.defaultgroup");
    }

    public int getLimit(String world, String category) {
        return config.getInt(world.toLowerCase() + ".categories." + category.toLowerCase() + ".limit", 1);
    }

    public int getTime(String world, String category) {
        return config.getInt(world.toLowerCase() + ".categories." + category.toLowerCase() + ".time", 1);
    }

    public Long getUserLastJoinInGroup(Player player, String group) {
        return Long.parseLong(history.getString(getPlayerWorldName(player).toLowerCase() + "." + player.getName().toLowerCase() + "." + group.toLowerCase() + ".lastjoin", "0"));
    }

    public ArrayList<String> getAllGroupsOfCategory(Player player, String category) {
        ArrayList<String> groups = new ArrayList<String>();
        for (Group g : getWorldDataOfPlayer(player).getGroupList()) {
            String cat = getGroupCategory(g);
            if (cat != null && cat.equalsIgnoreCase(category)) {
                groups.add(g.getName());
            }
        }
        return groups;
    }

    public ArrayList<String> getGroupRequirement(Group g) {
        ArrayList<String> requirements = new ArrayList<String>();
        if (g != null) {
            if (g.getVariables().hasVar(VARKEY_REQUIREMENT)) {
                Object reqObj = g.getVariables().getVarObject(VARKEY_REQUIREMENT);
                if (reqObj instanceof List) {
                    List<String> reqs = (List<String>) reqObj;
                    requirements.addAll(reqs);
                } else if (reqObj instanceof String) {
                    requirements.add((String) reqObj);
                }
            }
        }
        return requirements;
    }

    public String getGroupCategory(Group g) {
        if (g != null) {
            if (g.getVariables().hasVar(VARKEY_CATEGORY)) {
                //System.out.println("Returning "+ g.getVariables().getVarString(VARKEY_CATEGORY));
                return g.getVariables().getVarString(VARKEY_CATEGORY);
            }
        }
        return null;
    }

    public int getCountUserCategory(Player player, String category) {
        WorldDataHolder data = getWorldDataOfPlayer(player);
        String[] allGroups = data.getPermissionsHandler().getGroups(player.getName());
        int count = 0;
        for (String gName : allGroups) {
            Group g = data.getGroup(gName);
            String thisCategory = getGroupCategory(g);
            if (thisCategory != null && thisCategory.equalsIgnoreCase(category)) {
                count++;
            }
        }
        return count;
    }

    public WorldDataHolder getWorldDataOfPlayer(Player player) {
        return gm.getWorldsHolder().getWorldData(player);
    }

    public String getPlayerWorldName(Player player) {
        return getWorldDataOfPlayer(player).getName();
    }

    public boolean canUserLeaveGroup(Player player, String group) {
        String world = getPlayerWorldName(player);
        String category = getGroupCategory(getWorldDataOfPlayer(player).getGroup(group));
        if (category == null) {
            return false;
        }
        if (((getTime(world, category) * 3600000) + (getUserLastJoinInGroup(player, group))) < System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    public boolean doFulfillRequirements(Player player, String groupName) {
        WorldDataHolder worldData = getWorldDataOfPlayer(player);
        Group group = worldData.getGroup(groupName);
        boolean meetARequirement = true;
        for (String requirement : getGroupRequirement(group)) {
            boolean win = true;
            StringTokenizer groupTokenizer = new StringTokenizer(requirement, "&");
            while (groupTokenizer.hasMoreTokens()) {
                if (!worldData.getPermissionsHandler().inGroup(player.getName(), groupTokenizer.nextToken())) {
                    win = false;
                    break;
                }
            }
            if (win) {
                meetARequirement = true;
                break;
            } else {
                meetARequirement = false;
            }
        }
        //System.out.println("Testing requirement for "+groupName+" returned "+ meetARequirement);
        return meetARequirement;
    }

    public boolean canUserJoinGroup(Player player, String groupName) {
        String world = getPlayerWorldName(player);
        WorldDataHolder worldData = getWorldDataOfPlayer(player);
        User user = worldData.getUser(player.getName());
        Group group = worldData.getGroup(groupName);
        String category = getGroupCategory(group);
        //if category is null you cant join
        if (category == null) {
            return false;
        }
        //if user is already in the group, cant join
        if (worldData.getPermissionsHandler().inGroup(player.getName(), groupName)) {
            return false;
        }
        //if user has enough groups of that category, cant join
        int countCategory = getCountUserCategory(player, category);
        if (countCategory >= getLimit(world, category)) {
            return false;
        }
        //if user doesnt meet the requirements, cant join
        return doFulfillRequirements(player, groupName);
    }

    public ArrayList<String> getGroupsJoinableOfCategory(Player player, String category) {
        ArrayList<String> joinAble = new ArrayList<String>();
        ArrayList<String> allGroupsOfCategory = getAllGroupsOfCategory(player, category);
        for (String group : allGroupsOfCategory) {
            if (canUserJoinGroup(player, group)) {
                joinAble.add(group);
            }
        }
        return joinAble;
    }

    public ArrayList<String> getGroupsLeavableOfCategory(Player player, String category) {
        ArrayList<String> leaveAble = new ArrayList<String>();
        ArrayList<String> allGroupsOfCategory = getUserRolesInCategory(player, category);
        for (String group : allGroupsOfCategory) {
            if (canUserLeaveGroup(player, group)) {
                leaveAble.add(group);
            }
        }
        return leaveAble;
    }

    public ArrayList<String> getUserCategories(Player player) {
        ArrayList<String> categories = new ArrayList<String>();
        WorldDataHolder data = getWorldDataOfPlayer(player);
        String[] allGroups = data.getPermissionsHandler().getGroups(player.getName());
        for (String gName : allGroups) {
            Group g = data.getGroup(gName);
            String thisCategory = getGroupCategory(g);
            if (thisCategory != null && !categories.contains(thisCategory)) {
                categories.add(thisCategory);
            }
        }
        return categories;
    }

    public ArrayList<String> getUserRolesInCategory(Player player, String category) {
        ArrayList<String> roles = new ArrayList<String>();
        WorldDataHolder data = getWorldDataOfPlayer(player);
        String[] allGroups = data.getPermissionsHandler().getGroups(player.getName());
        for (String gName : allGroups) {
            Group g = data.getGroup(gName);
            String thisCategory = getGroupCategory(g);
            if (thisCategory != null && thisCategory.equalsIgnoreCase(category)) {
                roles.add(gName);
            }
        }
        return roles;
    }

    public ArrayList<String> getUserRoles(Player player) {
        ArrayList<String> roles = new ArrayList<String>();
        WorldDataHolder data = getWorldDataOfPlayer(player);
        String[] allGroups = data.getPermissionsHandler().getGroups(player.getName());
        for (String gName : allGroups) {
            Group g = data.getGroup(gName);
            String thisCategory = getGroupCategory(g);
            if (thisCategory != null && thisCategory.length() > 0) {
                roles.add(gName);
            }
        }
        return roles;
    }
}

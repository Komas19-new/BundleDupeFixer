package party.komas19.bundleDupeFixer;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class MessageManager {
    private final BundleDupeFixer plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(BundleDupeFixer plugin) {
        this.plugin = plugin;
        createMessagesFile();
    }

    private void createMessagesFile() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String msg = messagesConfig.getString(path, "&c[BundleDupeFixer] Missing message: " + path);
        msg = msg.replace("{prefix}", messagesConfig.getString("prefix", "&c[BundleDupeFixer] "));
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}

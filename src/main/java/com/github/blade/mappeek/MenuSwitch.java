package com.github.blade.mappeek;

import com.github.blade.hvac.api.EzvacApi;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * EZvac's Addons menu (EZvac API 1.9, BETA 1.0 Pilot 4): registers this
 * addon so a server can switch it on and off while it runs. Kept in a class
 * of its own, which is only loaded on an EZvac that has the menu, so the
 * addon still runs on older ones.
 */
final class MenuSwitch {
    private MenuSwitch() {}

    /** API 1.9 or newer, within major version 1. */
    static boolean offered(String apiVersion) {
        String[] parts = apiVersion.split("\\.");
        try {
            return parts.length >= 2 && Integer.parseInt(parts[0]) == 1 && Integer.parseInt(parts[1]) >= 9;
        } catch (NumberFormatException unknown) {
            return false;
        }
    }

    /**
     * Registers with the menu; {@code switcher} is called when the server
     * switches the addon. Returns whether it should run now.
     */
    static boolean register(EzvacApi ezvac, Plugin plugin, Consumer<Boolean> switcher) {
        return ezvac.registerAddon(plugin, switcher::accept).active();
    }
}

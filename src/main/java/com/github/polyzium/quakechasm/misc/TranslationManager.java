/*
 * Quakechasm, a Quake minigame plugin for Minecraft servers running PaperMC
 * 
 * Copyright (C) 2024-present Polyzium
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.polyzium.quakechasm.misc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import com.github.polyzium.quakechasm.QuakePlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TranslationManager {
    public static final Locale FALLBACK = Locale.US;
    private final HashMap<String, JsonObject> translations;
    private final MiniMessage miniMessage;
    public static TranslationManager INSTANCE = null;

    public TranslationManager() throws IOException {
        String languagesJson = new String(QuakePlugin.INSTANCE.getResource("lang.json").readAllBytes(), StandardCharsets.UTF_8);
        this.translations = new Gson().fromJson(languagesJson, new TypeToken<HashMap<String, JsonObject>>(){}.getType());
        this.miniMessage = MiniMessage.miniMessage();
        INSTANCE = this;
    }

    private String getTranslationString(String key, Locale locale) {
        JsonObject localeTranslations = translations.get(locale.getLanguage());
        if (localeTranslations == null) {
            localeTranslations = translations.get(FALLBACK.getLanguage());
        }

        String[] keyParts = key.split("\\.");
        JsonElement current = localeTranslations;

        for (String part : keyParts) {
            if (current == null || !current.isJsonObject()) {
                break;
            }
            current = current.getAsJsonObject().get(part);
        }

        if (current != null && current.isJsonPrimitive()) {
            return current.getAsString();
        }

        // Fallback to FALLBACK locale
        if (!locale.getLanguage().equals(FALLBACK.getLanguage())) {
            JsonObject fallbackTranslations = translations.get(FALLBACK.getLanguage());
            current = fallbackTranslations;

            for (String part : keyParts) {
                if (current == null || !current.isJsonObject()) {
                    break;
                }
                current = current.getAsJsonObject().get(part);
            }

            if (current != null && current.isJsonPrimitive()) {
                QuakePlugin.INSTANCE.getLogger().warning("TranslationManager could not get translation of " + key + " for locale " + locale.getLanguage() + ", falling back to " + FALLBACK.getLanguage());
                return current.getAsString();
            }
        }

        QuakePlugin.INSTANCE.getLogger().severe("TranslationManager could not get translation of " + key);
        return key;
    }

    public Component translate(String key, Locale locale, TagResolver... placeholders) {
        String translatedString = getTranslationString(key, locale);
        if (placeholders.length > 0) {
            return miniMessage.deserialize(translatedString, placeholders);
        }
        return miniMessage.deserialize(translatedString);
    }

    public Component translate(String key, Player player, TagResolver... placeholders) {
        return translate(key, player.locale(), placeholders);
    }

    public String translateLegacy(String key, Locale locale) {
        return getTranslationString(key, locale);
    }

    public String translateLegacy(String key, Player player) {
        return getTranslationString(key, player.locale());
    }

    public static Component t(String key, Locale locale, TagResolver... placeholders) {
        return INSTANCE.translate(key, locale, placeholders);
    }

    public static Component t(String key, Player player, TagResolver... placeholders) {
        return INSTANCE.translate(key, player, placeholders);
    }

    public static String tLegacy(String key, Locale locale) {
        return INSTANCE.translateLegacy(key, locale);
    }

    public static String tLegacy(String key, Player player) {
        return INSTANCE.translateLegacy(key, player);
    }
}

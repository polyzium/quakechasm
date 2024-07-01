package ru.darkchronics.quake.misc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import ru.darkchronics.quake.QuakePlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

public class TranslationManager {
    public static final Locale FALLBACK = Locale.US;
    public static class Translation extends HashMap<String, String> {}
    public HashMap<String, Translation> translations;
    public static TranslationManager INSTANCE = null;

    public TranslationManager() throws IOException {
        String languagesJson = new String(QuakePlugin.INSTANCE.getResource("lang.json").readAllBytes(), StandardCharsets.UTF_8);
        this.translations = new Gson().fromJson(languagesJson, new TypeToken<HashMap<String, Translation>>(){}.getType());
        INSTANCE = this;
    }

    public String translate(String key, Locale locale) {
        String translatedString = translations.get(locale.getLanguage()).get(key);
        if (translatedString == null) {
            String translatedStringFallback = translations.get(FALLBACK.getLanguage()).get(key);
            if (translatedStringFallback == null) {
                QuakePlugin.INSTANCE.getLogger().severe("TranslationManager could not get translation of "+key);
                return key;
            }
            QuakePlugin.INSTANCE.getLogger().warning("TranslationManager could not get translation of "+key+" for locale "+locale.getLanguage()+", falling back to "+FALLBACK.getLanguage());
            return translatedStringFallback;
        }
        return translatedString;
    }

    public String translate(String key, Player player) {
        Locale locale = player.locale();
        return this.translate(key, locale);
    }

    public static String t(String key, Locale locale) {
        return INSTANCE.translate(key, locale);
    }

    public static String t(String key, Player player) {
        return INSTANCE.translate(key, player);
    }
}

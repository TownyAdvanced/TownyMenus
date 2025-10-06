package io.github.townyadvanced.townymenus.utils;

import net.kyori.adventure.translation.Translator;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public class Localization {
    @NotNull
    public static Optional<Locale> parseLocale(@NotNull String locale) {
        return Optional.ofNullable(Translator.parseLocale(locale));
    }

    @NotNull
    public static Optional<Locale> parseLocale(@Nullable Player player) {
        return player != null ? parseLocale(player.getLocale()) : Optional.empty();
    }

    @NotNull
    public static Locale localeOrDefault(@Nullable Player player) {
        return parseLocale(player).orElse(Translation.getDefaultLocale());
    }
}

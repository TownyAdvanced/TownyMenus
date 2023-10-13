package io.github.townyadvanced.townymenus.utils;

import com.palmergames.util.JavaUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.Translator;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Localization {
    private static final Map<Locale, ComponentFlattener> flatteners = new HashMap<>();
    private static final Pattern LOCALIZATION_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?[sd]");
    private static final MethodHandle TRANSLATIONS_HANDLE = JavaUtil.getFieldHandle(Translation.class, "translations");

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

    @ApiStatus.Internal
    public static Translator newTranslator() {
        return new Translator() {
            @Override
            public @NotNull Key name() {
                return Key.key("townymenus", "translator");
            }

            @Override
            public @Nullable MessageFormat translate(final @NotNull String key, final @NotNull Locale locale) {
                return null;
            }

            @Override
            public @Nullable Component translate(final @NotNull TranslatableComponent component, final @NotNull Locale locale) {
                if (!Translation.hasTranslation(component.key(), locale))
                    return null;

                final TextComponent.Builder builder = Component.text().mergeStyle(component);

                flattener(locale).flatten(component, new FlattenerListener() {
                    Style currentStyle = Style.empty();

                    @Override
                    public void pushStyle(@NotNull Style style) {
                        currentStyle = style;
                    }

                    @Override
                    public void component(@NotNull String text) {
                        builder.append(Component.text(text, currentStyle));
                    }

                    @Override
                    public void popStyle(@NotNull Style style) {
                        currentStyle = Style.empty();
                    }
                });

                return builder.build();
            }
        };
    }

    private static ComponentFlattener flattener(final @NotNull Locale locale) {
        return flatteners.computeIfAbsent(locale, k -> ComponentFlattener.basic().toBuilder()
                .complexMapper(TranslatableComponent.class, (translatable, consumer) -> {
                    final String translated = Translation.of(translatable.key(), locale);

                    final Matcher matcher = LOCALIZATION_PATTERN.matcher(translated);
                    final List<Component> args = translatable.args();
                    int argPosition = 0;
                    int lastIdx = 0;
                    while (matcher.find()) {
                        // append prior
                        if (lastIdx < matcher.start()) {
                            consumer.accept(MiniMessage.miniMessage().deserialize(translated.substring(lastIdx, matcher.start())));
                        }
                        lastIdx = matcher.end();

                        final String argIdx = matcher.group(1);
                        // calculate argument position
                        if (argIdx != null) {
                            try {
                                final int idx = Integer.parseInt(argIdx) - 1;
                                if (idx < args.size()) {
                                    consumer.accept(args.get(idx));
                                }
                            } catch (final NumberFormatException ex) {
                                // ignore, drop the format placeholder
                            }
                        } else {
                            final int idx = argPosition++;
                            if (idx < args.size()) {
                                consumer.accept(args.get(idx));
                            }
                        }
                    }

                    // append tail
                    if (lastIdx < translated.length()) {
                        consumer.accept(MiniMessage.miniMessage().deserialize(translated.substring(lastIdx)));
                    }
                })
                .build());
    }
}

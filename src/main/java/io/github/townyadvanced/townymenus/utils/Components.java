package io.github.townyadvanced.townymenus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class Components {
    public static Component toNative(final com.palmergames.adventure.text.@NotNull Component component) {
        return GsonComponentSerializer.gson().deserialize(com.palmergames.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(component));
    }
}

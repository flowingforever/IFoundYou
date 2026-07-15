package pro.fazeclan.river.ifoundyou.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextUtil {

    public static Component formatComponent(String message) {
        return MiniMessage.miniMessage().deserialize("<red>Found You!</red> <gray>></gray> " + message);
    }

    public static Set<String> stringListToSet(String input) {
        return new HashSet<>(List.of(input.replaceAll(" ", "").split(",")));
    }

}

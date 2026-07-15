package pro.fazeclan.river.ifoundyou.dialog;

import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.role.Faction;
import pro.fazeclan.river.ifoundyou.role.Role;
import pro.fazeclan.river.ifoundyou.util.TextUtil;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class RoleCreationDialog {

    public static void dialog(Player player) {
        var miniMessage = MiniMessage.miniMessage();
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase
                        .builder(TextUtil.formatComponent("<blue>Role</blue> <gray>></gray> <yellow>Creation</yellow>"))
                        .body(List.of(
                                DialogBody.plainMessage(miniMessage.deserialize(
                                        "<gray>You can cancel this at any point by simply pressing your escape key.</gray>"
                                )),
                                DialogBody.plainMessage(miniMessage.deserialize(
                                        "<gray>Do also note that your current inventory will be used for the role's inventory.</gray>"
                                ))
                        ))
                        .inputs(List.of(
                                DialogInput.text("name", Component.text("Name of Your Role")).build(),
                                DialogInput.text("key", miniMessage.deserialize("<hover:show_text:'This must be unique to this role, all lowercase, and use underscores instead of spaces.'>Role ID")).build(),
                                DialogInput.text("description", 200, Component.text("Description"), true, "hai :3", 1024, null),
                                DialogInput.singleOption("faction", Component.text("Faction"), List.of(
                                        SingleOptionDialogInput.OptionEntry.create("runners", Component.text("Runners"), true),
                                        SingleOptionDialogInput.OptionEntry.create("hunters", Component.text("Hunters"), false)
                                )).build(),
                                DialogInput.numberRange("max_players", miniMessage.deserialize("<hover:show_text:'The maximum number of players that can have this role! Set it to -1 for an unlimited amount of people to have this role!'>Max Players"), -1f, 20f).step(1f).initial(-1f).build(),
                                DialogInput.text("abilities", miniMessage.deserialize("<hover:show_text:'A comma separated list of ability IDs.'>Abilities")).build()
                        ))
                        .build()
                )
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Confirm").color(NamedTextColor.GREEN),
                                null,
                                100,
                                DialogAction.customClick(
                                        (response, audience) -> {
                                            var manager = IFoundYou.getInstance().getRoleManager();
                                            var name = response.getText("name");
                                            var id = response.getText("key");
                                            var faction = Faction.valueOf(response.getText("faction").toUpperCase());
                                            var maxPlayers = response.getFloat("max_players").intValue();
                                            var abilities = TextUtil.stringListToSet(response.getText("abilities"));
                                            var description = response.getText("description");
                                            manager.createRole(new Role(
                                                    name,
                                                    id,
                                                    NBT.get(
                                                            player,
                                                            nbt -> "{Inventory:" + nbt.getCompoundList("Inventory") + ", equipment:" + nbt.getCompound("equipment") + "}"
                                                    ),
                                                    faction,
                                                    maxPlayers,
                                                    abilities,
                                                    description
                                            ));
                                        },
                                        ClickCallback.Options.builder().build()
                                )
                        ),
                        ActionButton.create(
                                Component.text("Cancel").color(NamedTextColor.RED),
                                null,
                                100,
                                null
                        )
                ))
        ));
    }

}

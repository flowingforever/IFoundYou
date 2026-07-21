package pro.fazeclan.river.ifoundyou.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.dialog.RoleCreationDialog;

public class RoleCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("role")
                .requires(ctx -> ctx.getSender().hasPermission("found_you.admin.role"))
                .then(
                        Commands.literal("create")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    RoleCreationDialog.dialog(player);

                                    return Command.SINGLE_SUCCESS;
                                })
                )
                .then(
                        Commands.literal("reload")
                                .executes(ctx -> {
                                    var manager = IFoundYou.getInstance().getRoleManager();
                                    manager.reloadRegistry();

                                    return Command.SINGLE_SUCCESS;
                                })
                );
    }

}

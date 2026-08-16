package com.racks.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.racks.RacksPlugin;
import com.racks.item.RackItems;
import com.racks.lang.LanguageManager;
import com.racks.model.RackVariant;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * {@code /racks} — hand out racks and reload the plugin.
 *
 * <p>Registered through Paper's Brigadier API, which gives real argument types, per-branch permission
 * checks and tab completion that only offers what the sender can actually run.
 */
public final class RacksCommand {

    private static final String PERM_GIVE = "racks.command.give";
    private static final String PERM_RELOAD = "racks.command.reload";

    private final RacksPlugin plugin;

    public RacksCommand(RacksPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("racks")
                .requires(source -> source.getSender().hasPermission(PERM_GIVE)
                        || source.getSender().hasPermission(PERM_RELOAD))
                .then(giveBranch())
                .then(reloadBranch())
                .build();
    }

    /** Aliases the command answers to, alongside its own name. */
    public static List<String> aliases() {
        return List.of("rack");
    }

    // ------------------------------------------------------------------------------------------------
    // /racks give <variant> [player] [count]
    // ------------------------------------------------------------------------------------------------

    private LiteralArgumentBuilder<CommandSourceStack> giveBranch() {
        return Commands.literal("give")
                .requires(source -> source.getSender().hasPermission(PERM_GIVE))
                .then(Commands.argument("variant", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
                            for (RackVariant variant : RackVariant.values()) {
                                if (variant.id().startsWith(prefix)) {
                                    builder.suggest(variant.id());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> give(ctx, null, 1))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
                                    for (Player online : Bukkit.getOnlinePlayers()) {
                                        if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                                            builder.suggest(online.getName());
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> give(ctx, StringArgumentType.getString(ctx, "player"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> give(ctx,
                                                StringArgumentType.getString(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "count"))))));
    }

    private int give(CommandContext<CommandSourceStack> ctx, @Nullable String targetName, int count) {
        CommandSender sender = ctx.getSource().getSender();
        LanguageManager lang = plugin.languageManager();

        String variantId = StringArgumentType.getString(ctx, "variant");
        RackVariant variant = RackVariant.byId(variantId);
        if (variant == null) {
            sender.sendMessage(lang.get("command.unknown-variant", "variant", variantId));
            return 0;
        }

        Player target;
        if (targetName != null) {
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(lang.get("command.player-not-found", "player", targetName));
                return 0;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(lang.get("command.only-player"));
            return 0;
        }

        RackItems items = plugin.rackItems();
        int dropped = items.giveOrDrop(target, items.create(variant, target.locale(), count));

        // The wood name goes in as a component rather than as text, so it resolves in the language of
        // whoever reads the message — giver and receiver need not share one.
        Component woodName = lang.get("item.variant." + variant.id());
        String amount = Integer.toString(count);

        if (target.equals(sender)) {
            sender.sendMessage(lang.getArgs("command.give-self",
                    LanguageManager.arg("count", amount),
                    LanguageManager.arg("variant", woodName)));
        } else {
            sender.sendMessage(lang.getArgs("command.give-other",
                    LanguageManager.arg("count", amount),
                    LanguageManager.arg("variant", woodName),
                    LanguageManager.arg("player", target.getName())));
            target.sendMessage(lang.getArgs("command.give-received",
                    LanguageManager.arg("count", amount),
                    LanguageManager.arg("variant", woodName)));
        }
        if (dropped > 0) {
            target.sendMessage(lang.get("command.give-dropped", "dropped", Integer.toString(dropped)));
        }
        return 1;
    }

    // ------------------------------------------------------------------------------------------------
    // /racks reload
    // ------------------------------------------------------------------------------------------------

    private LiteralArgumentBuilder<CommandSourceStack> reloadBranch() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission(PERM_RELOAD))
                .executes(ctx -> {
                    plugin.reloadPlugin();
                    ctx.getSource().getSender().sendMessage(plugin.languageManager().get("command.reloaded"));
                    return 1;
                });
    }
}

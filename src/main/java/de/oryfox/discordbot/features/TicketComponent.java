package de.oryfox.discordbot.features;

import de.oryfox.discordbot.commands.CommandProvider;
import de.oryfox.discordbot.model.FeatureConfigurator;
import de.oryfox.discordbot.persistence.PersistenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.modals.Modal;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketComponent extends ListenerAdapter implements CommandProvider, FeatureConfigurator {

    private final JDA jda;
    private final PersistenceService persistenceService;

    private final Timer timer = new Timer();

    private static final String COMMAND_NAME = "createticketmessage";
    private static final String BUTTON_ID_CREATE = "ticket_create";
    private static final String BUTTON_ID_CLOSE = "ticket_close";

    private static final String MODAL_ID = "ticket_create_modal";
    private static final String MODAL_TYPE = "ticket_type";
    private static final String MODAL_DESCRIPTION = "ticket_description";

    @PostConstruct
    public void initTicketComponent() {
        jda.addEventListener(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
            if (config.isTicketSystemEnabled()) {
                event.reply(":thumbsup:").setEphemeral(true).queue();
                event.getChannel().sendMessageComponents(createTicketMessageContainer()).useComponentsV2().queue();
            } else {
                event.reply("The ticketing system is not enabled.").queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        if (event.getButton().getCustomId().equalsIgnoreCase(BUTTON_ID_CREATE)) {
            if (config.isTicketSystemEnabled()) {
                event.replyModal(createTicketModal()).queue();
            } else {
                event.reply("The ticketing system is not enabled.").queue();
            }
        }
        if (event.getButton().getCustomId().equalsIgnoreCase(BUTTON_ID_CLOSE)) {
            event.editButton(event.getButton().asDisabled()).queue();
            event.getChannel().sendMessage(String.format("<@%s> closed this ticket. It will be deleted shortly. :thumbsup:", event.getUser().getId())).queue();
            var userId = event.getChannel().asTextChannel().getPermissionContainer().getMemberPermissionOverrides().getFirst().getPermissionHolder().getIdLong();

            var manager = event.getChannel().asTextChannel().getManager();
            manager.putMemberPermissionOverride(userId, List.of(Permission.VIEW_CHANNEL), List.of(Permission.MESSAGE_SEND)).queue();

            final var channel = event.getChannel().asTextChannel();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    channel.delete().queue();
                }
            }, 600000);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equalsIgnoreCase(MODAL_ID)) {
            event.reply("Your ticket has been created!").setEphemeral(true).queue();
            TextChannel channel;

            channel = event.getChannel().asTextChannel().getParentCategory().createTextChannel(String.format("ticket-%s", event.getUser().getName())).complete();
            var everyone = channel.getGuild().getPublicRole();
            channel.getManager()
                    .putPermissionOverride(everyone, List.of(), List.of(Permission.VIEW_CHANNEL))
                    .putMemberPermissionOverride(event.getMember().getIdLong(), List.of(Permission.VIEW_CHANNEL), List.of())
                    .complete();
            channel.sendMessageComponents(createTicketCreatedMessageContainer(event.getUser().getId(), String.join(", ", event.getValue(MODAL_TYPE).getAsStringList()), event.getValue(MODAL_DESCRIPTION).getAsString())).useComponentsV2().queue();
        }
    }

    @Override
    public List<CommandData> getCommands() {
        return List.of(Commands.slash(COMMAND_NAME, "Create the message for submitting tickets in this channel.")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .setContexts(InteractionContextType.GUILD));
    }

    private Container createTicketMessageContainer() {
        return Container.of(
                TextDisplay.of("## Ticket System"),
                Separator.createInvisible(Separator.Spacing.SMALL),
                TextDisplay.of("Here you can submit a ticket and start a conversation with the discord server's staff. You can report users' behaviour, give feedback to the server team or whatever comes to your mind."),
                ActionRow.of(Button.primary(BUTTON_ID_CREATE, "Open ticket"))
        );
    }

    private Modal createTicketModal() {
        return Modal.create(MODAL_ID, "Creating a ticket")
                .addComponents(
                        Label.of("What type of ticket are you opening?",
                        StringSelectMenu.create(MODAL_TYPE)
                                .setRequired(true)
                                .addOption("Report", "REPORT")
                                .addOption("Feedback", "FEEDBACK")
                                .addOption("Other", "OTHER")
                                .build()),
                        Label.of("Please describe your request.", TextInput.create(MODAL_DESCRIPTION, TextInputStyle.PARAGRAPH).setRequired(true).build()))
                .build();
    }

    private Container createTicketCreatedMessageContainer(String userId, String type, String description) {
        return Container.of(
                TextDisplay.of("### Ticket summary"),
                Separator.createInvisible(Separator.Spacing.SMALL),
                TextDisplay.ofFormat("User <@%s> created a ticket of type `%s` with the following description:", userId, type),
                TextDisplay.of(description),
                Separator.createDivider(Separator.Spacing.SMALL),
                Section.of(
                        Button.danger(BUTTON_ID_CLOSE, "Close ticket"),
                        TextDisplay.of("You may close your own ticket if you feel like it.")
                ),
                Separator.createInvisible(Separator.Spacing.SMALL),
                TextDisplay.of(String.format("-# <t:%s:f>", Instant.now().getEpochSecond()))
        );
    }

    @Override
    public String getFeatureName() {
        return "Ticket System";
    }

    @Override
    public List<Command.Choice> getActions() {
        return List.of(
                new Command.Choice("Disable", "disable"),
                new Command.Choice("Enable", "enable")
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        var action = event.getOption("action").getAsString();
        switch (action) {
            case "enable" -> {
                config.setTicketSystemEnabled(true);
                event.reply("The ticket system feature is now enabled").setEphemeral(true).queue();
            }
            case "disable" -> {
                config.setTicketSystemEnabled(false);
                event.reply("The ticket system feature is now disabled").setEphemeral(true).queue();
            }
            default -> event.replyFormat("Unknown action %s for feature %s", action, getFeatureName()).setEphemeral(true).queue();
        }
        persistenceService.persist(config);
    }
}

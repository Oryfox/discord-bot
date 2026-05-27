package de.oryfox.discordbot.component;

import de.oryfox.discordbot.commands.CommandProvider;
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
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.modals.Modal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketComponent extends ListenerAdapter implements CommandProvider {

    private final JDA jda;

    @Value("${channels.ticket.category:}")
    private String categoryId;

    @Value("${role.ticket.ping:}")
    private String roleId;

    private Category category;
    private Role role;

    private int ticketNo = 1;
    private final Timer timer = new Timer();

    private static final String COMMAND_NAME = "createticketmessage";
    private static final String BUTTON_ID_CREATE = "ticket_create";
    private static final String BUTTON_ID_CLOSE = "ticket_close";

    private static final String MODAL_ID = "ticket_create_modal";
    private static final String MODAL_TYPE = "ticket_type";
    private static final String MODAL_DESCRIPTION = "ticket_description";

    @PostConstruct
    public void initTicketComponent() {
        if (categoryId == null || categoryId.isBlank()) {
            log.warn("No category id for the ticket system was provided. Using server root for ticket channels.");
        } else {
            category = jda.getCategoryById(categoryId);
            if (category == null) {
                log.warn("Provided category id for the ticket system appears to be wrong. Fallback to using server root for ticket channels.");
            }
        }
        if (roleId == null || roleId.isBlank()) {
            log.warn("No specific role id set for moderators/team members. The tickets will only be visible to those with sufficient default permissions.");
        } else {
            role = jda.getRoleById(roleId);
            if (role == null) {
                log.warn("Provided role id for the ticket system appears to be wrong. The tickets will only be visible to those with sufficient default permissions.");
            }
        }
        jda.addEventListener(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            event.reply(":thumbsup:").setEphemeral(true).queue();
            event.getChannel().sendMessageComponents(createTicketMessageContainer()).useComponentsV2().queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getButton().getCustomId().equalsIgnoreCase(BUTTON_ID_CREATE)) {
            event.replyModal(createTicketModal()).queue();
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
            if (category != null) {
                channel = category.createTextChannel("Ticket #" + ticketNo++).complete();
            } else {
                channel = event.getGuild().createTextChannel("ticket-" + ticketNo++).complete();
            }
            var everyone = channel.getGuild().getPublicRole();
            var action = channel.getManager()
                    .putPermissionOverride(everyone, List.of(), List.of(Permission.VIEW_CHANNEL))
                    .putMemberPermissionOverride(event.getMember().getIdLong(), List.of(Permission.VIEW_CHANNEL), List.of());

            if (role != null) {
                action = action.putRolePermissionOverride(role.getIdLong(), List.of(Permission.MANAGE_CHANNEL), List.of());
            }
            action.complete();
            channel.sendMessageComponents(createTicketCreatedMessageContainer(event.getUser().getId(), String.join(", ", event.getValue(MODAL_TYPE).getAsStringList()), event.getValue(MODAL_DESCRIPTION).getAsString())).useComponentsV2().queue();
        }
    }

    @Override
    public List<SlashCommandData> getCommands() {
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
}

package org.example;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.json.HTTP;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpRequest;
import java.util.List;


public class Main extends ListenerAdapter
{

    private static DatabaseManager db;

    public static void main(String[] args) throws LoginException, InterruptedException
    {
        Dotenv dot = Dotenv.load();

        db = new DatabaseManager();


        String token = dot.get("DISCORD_BOT_TOKEN");

        JDA bot = JDABuilder.createDefault(token)
                .enableIntents(
                        net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES,
                        net.dv8tion.jda.api.requests.GatewayIntent.MESSAGE_CONTENT
                )
                .addEventListeners(new Main())
                .build();

        bot.awaitReady();

        Guild guild = bot.getGuildById(dot.get("GUILD_ID"));

        if (guild != null)
        {
            guild.upsertCommand("adduser", "Adds a user to the database")
                    .addOption(OptionType.STRING, "profile", "User profile name", true)
                    .addOption(OptionType.STRING, "email", "User email", true)
                    .addOption(OptionType.STRING, "discord_id", "User Discord ID", true)
                    .queue();
        }

        System.out.println("Waiting for messages");
    }


    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        if (event.getName().equals("adduser"))
        {
            String profile = event.getOption("profile").getAsString();
            String email = event.getOption("email").getAsString();
            String discordID = event.getOption("discord_id").getAsString();

            boolean inserted = db.insertUser(discordID, profile, email);

            if (inserted)
            {
                event.reply("User added to database successfully!").setEphemeral(true).queue();
            }
            else
            {
                event.reply("User was not added to database!").setEphemeral(true).queue();
            }
        }
    }


    public void onMessageReceived(MessageReceivedEvent event)
    {
        String dotChannel = Dotenv.load().get("CHANNEL_ID");

        if (!event.getChannel().getId().equals(dotChannel))
        {
            return;
        }

        Message msg = event.getMessage();

        System.out.println(msg.getAuthor().getName());
        System.out.println(msg.getContentRaw());

        if (msg.getEmbeds().isEmpty())
        {
            return;
        }

        for (MessageEmbed embed : msg.getEmbeds())
        {
            {
                System.out.println(embed.getTitle());
                System.out.println(embed.getDescription());

                String itemName = embed.getTitle() != null
                        ? stripMarkdownLink(embed.getTitle())
                        : "Unknown Item";

                String site = null;
                String profile = null;
                String account = null;

                for (MessageEmbed.Field field : embed.getFields())
                    switch (field.getName())
                    {
                        case "Account" :
                            account = field.getValue();
                            break;
                        case "Profile":
                            profile = field.getValue();
                            break;
                        case "Site":
                            site = field.getValue();
                            break;
                    }

                if (site == null)
                {
                    site = "Amazon.com";
                }

                profile = removeSpoilerTag(profile);
                account = removeSpoilerTag(account);

                System.out.println("Site: " + site);
                System.out.println("Profile: " + profile);
                System.out.println("Account: " + account);

                String webhookURL = Dotenv.load().get("DISCORD_URL");
                DiscordWebhook webhook = new DiscordWebhook(webhookURL);
                String userToMention = null;

                if (site.equals("Amazon.com"))
                {
                    userToMention = db.getDiscordIDbyEmail(account);
                }
                else
                {
                    userToMention = db.getDiscordIDbyProfile(profile);
                }

                webhook.sendCheckoutSuccess(userToMention, embed.getDescription(), site);
            }
        }

    }

    private static String stripMarkdownLink(String markdown) {
        return markdown.replaceAll("\\[(.*?)]\\((.*?)\\)", "$1");
    }

    private static String removeSpoilerTag(String input)
    {
        try
        {
            return input.replaceAll("\\|\\|", "");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }
}
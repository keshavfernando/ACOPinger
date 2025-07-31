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

        checkMessage(msg);


        if (msg.getEmbeds().isEmpty())
        {
            return;
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

    private static boolean checkOrder(String input)
    {
        if (input.equals("Successful Checkout (Review Hold)") || input.equals("Successful Checkout!"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private static void checkMessage(Message msg)
    {
        if (msg.getEmbeds().isEmpty())
        {
            addToDB(msg);
        }
        else
        {
            checkWebhook(msg);
        }
    }

    private static void checkWebhook(Message msg)
    {
        for (MessageEmbed embed : msg.getEmbeds())
        {
            {
                System.out.println(embed.getTitle());
                System.out.println(embed.getDescription());


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
                String itemCheckedOut = null;

                if (site.equals("Amazon.com"))
                {
                    userToMention = db.getDiscordIDbyEmail(account);
                    itemCheckedOut = embed.getDescription();
                    webhook.sendCheckoutSuccess(userToMention, embed.getDescription(), site);
                    System.out.println("Checkout webhook sent");
                }
                else if (site.equals("Pokemon Center US"))
                {
                    userToMention = db.getDiscordIDbyProfile(profile);
                    itemCheckedOut = "Pokemon Center item checked Out!";
                    webhook.sendCheckoutSuccess(userToMention, embed.getDescription(), site);
                    System.out.println("Checkout webhook sent");
                }
                else
                {
                    userToMention = db.getDiscordIDbyProfile(profile);
                    itemCheckedOut = embed.getDescription();

                    if (checkOrder(embed.getTitle()))
                    {
                        webhook.sendCheckoutSuccess(userToMention, embed.getDescription(), site);
                        System.out.println("Checkout webhook sent");
                    }
                    else
                    {
                        webhook.sendCheckoutFailure(userToMention, embed.getDescription(), site);
                        System.out.println("Failure webhook sent");
                    }
                }
            }
        }
    }

    private static void addToDB(Message msg)
    {
        String data = msg.getContentRaw();

        String[] lines = data.split("\n");


        for (String line : lines)
        {
            System.out.println("Starting work on line: " + line);

            String[] userData = line.trim().split(",", 3);

            if (userData.length == 3)
            {
                String profile = userData[0];
                String email = userData[1];
                String discordID = userData[2];

                db.insertUser(discordID,profile,email);
                System.out.println("Users added to database: " + profile + ": " + email);
            }
        }
    }
}
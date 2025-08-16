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
import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main extends ListenerAdapter
{

    private static DatabaseManager db;
    private static Dotenv dot;
    private static final Pattern pattern = Pattern.compile("^([^,]+),([^,]+),([^,]+)$");
    private static final Pattern emailPattern = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static Matcher matcher;


    private static Set<String> declineTitle = Set.of(
            "Order Canceled: Item Demand",
            "Order Canceled: Reseller",
            "Your card was declined"
    );

    public static void main(String[] args) throws LoginException, InterruptedException
    {
        dot = Dotenv.configure()
                .directory("C:\\Users\\Administrator\\Downloads\\ACOPinger-master\\ACOPinger-master")
                .filename(".env")
                .load();

        Path dbPath = Paths.get(dot.get("DB_PATH")).toAbsolutePath().normalize();
        String DB_URL = "jdbc:sqlite:" + dbPath.toString();

        db = new DatabaseManager(DB_URL);


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

        String dotChannel = dot.get("CHANNEL_ID");

        if (!event.getChannel().getId().equals(dotChannel))
        {
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        Message msg = event.getMessage();

        checkMessage(msg, channel);


        if (msg.getEmbeds().isEmpty())
        {
            return;
        }

    }

    public static String stripMarkdownLink(String markdown) {
        return markdown.replaceAll("\\[(.*?)]\\((.*?)\\)", "$1");
    }

    public static String removeSpoilerTag(String input)
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

    public static String resolveUserID(String profile, String email, String account, DatabaseManager db)
    {
        if (profile != null && !profile.isBlank())
        {
            return db.getDiscordIDbyProfile(profile);
        }
        else if (account != null && !account.isBlank())
        {
            return db.getDiscordIDbyEmail(account);
        }
        else
        {
            return db.getDiscordIDbyEmail(email);
        }
    }

    public static String resolveItem(String product, String productOne, String description, String item)
    {
        if (product != null && !product.isBlank())
        {
            return product;
        }
        else if (productOne != null && !productOne.isBlank())
        {
            return productOne;
        }
        else if (item != null && !item.isBlank())
        {
            return item;
        }
        else
        {
            return description;
        }
    }

    public static boolean checkDecline(String input)
    {
        return declineTitle.contains(input);
    }

    public static void checkMessage(Message msg, TextChannel channel)
    {
        if (msg.getEmbeds().isEmpty())
        {
            checkAndAdd(msg, channel);
        }
        else
        {
            checkWebhook(msg);
        }
    }

    public static void checkWebhook(Message msg)
    {
        for (MessageEmbed embed : msg.getEmbeds())
        {
            {
                String title = embed.getTitle();
                String description = embed.getDescription();
                String profile = null;
                String account = null;
                String item = null;
                String email = null;
                String Product = null;
                String productOne = null;

                System.out.println(title);
                System.out.println(description);

                for (MessageEmbed.Field field : embed.getFields())
                    switch (field.getName())
                    {
                        case "Account" :
                            account = field.getValue();
                            break;
                        case "Profile":
                            profile = field.getValue();
                            break;
                        case "Email":
                            email = field.getValue();
                            break;
                        case "Item":
                            item = field.getValue();
                            break;
                        case "Product":
                            Product = field.getValue();
                            break;
                        case "Product (1)":
                            productOne = field.getValue();
                            break;
                    }

                profile = removeSpoilerTag(profile);
                account = removeSpoilerTag(account);
                email =removeSpoilerTag(email);

                System.out.println("Profile: " + profile);
                System.out.println("Account: " + account);
                System.out.println("Email: " + email);
                System.out.println("Item: " + item);
                System.out.println("Product: " + Product);
                System.out.println("Product (1): " + productOne);

                String webhookURL = dot.get("DISCORD_URL");
                DiscordWebhook webhook = new DiscordWebhook(webhookURL);
                String userToMention = null;
                String itemCheckedOut = null;


                if (checkDecline(title))
                {
                    System.out.println("Item is a decline webhook");
                    userToMention = db.getDiscordIDbyProfile(profile);
                    itemCheckedOut = description;
                    webhook.sendCheckoutFailure(userToMention, itemCheckedOut);
                    System.out.println("Checkout failure sent");
                }

                else
                {
                    System.out.println("Item is not a decline webhook");
                    userToMention = resolveUserID(profile, email, account, db);
                    itemCheckedOut = resolveItem(Product, productOne, description, item);
                    webhook.sendCheckoutSuccess(userToMention, itemCheckedOut);
                    System.out.println("Checkout failure sent");
                }

            }
        }
    }

    private static void checkAndAdd(Message msg, TextChannel channel)
    {
        String data = msg.getContentRaw();

        String[] lines = data.split("\n");

        for (String line : lines)
        {

            Matcher matcher = pattern.matcher(line);

            if (matcher.matches()) {
                String profileName = matcher.group(1).trim();
                String email = matcher.group(2).trim();
                String discordID = matcher.group(3).trim();

                if (emailPattern.matcher(email).matches()) {
                    boolean complete = db.insertUser(discordID, profileName, email);
                    if (complete) {
                        channel.sendTyping().queue();
                        channel.sendMessage("✅ User: " + email + " has been added to database").queue();
                    } else {
                        channel.sendTyping().queue();
                        channel.sendMessage("❌ User: " + email + " has NOT been added to database").queue();
                    }
                } else {
                    System.out.println("Bad format");
                }
            }
        }
    }
}
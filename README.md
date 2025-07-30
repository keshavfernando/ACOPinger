# ACOPinger

This script is a Discord bot built with Java and SQLite that listens for incoming webhooks and notifies customers for successfil checkouts. Webhooks are read from a variety of bots, and then forwaded to ACO servers. This tool is perfect for ACO runners who would like to keep track of their customer's checkout numbers.

🚀 Features
- Listens for incoming webhooks
- Notifies clients in seperate Discord servers with pings and item names
- Simple SQLite database integration to keep track of client and webhook tracking
- Easily configurable with slash commands to add users.

📦 Built with
- Java 17
- Maven
- Discord JDA
- SQLite

🛠️ Setup
1. In the root folder, please make sure to create a '.env' file. Within this environment file, place three keys: DISCORD_BOT_TOKEN, CHANNEL_ID, GUILD_ID, and DISCORD_URL. The bot token is the Discord bot token. The channel ID is the channel to be monitored. The Guild ID is the webhook server being monitored. Finally, the discord URL is your webhook URL.
2. Lastly, create a 'data' folder, with a 'user_data.db' file to store user data.
3. Add users to your database through using the /adduser command in Discord.
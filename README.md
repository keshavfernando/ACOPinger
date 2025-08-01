# ACOPinger

This script is a Discord bot built with Java and SQLite that listens for incoming webhooks and notifies customers for successfil checkouts. Webhooks are read from a variety of bots, and then forwaded to ACO servers. This tool is perfect for ACO runners who would like to keep track of their customer's checkout numbers.

---


## 🚀 Features
- Listens for incoming webhooks
- Notifies clients in seperate Discord servers with pings and item names
- Simple SQLite database integration to keep track of client and webhook tracking
- Easily configurable with slash commands to add users.
- Bulk profile addition through channel messages


---

## 📦 Built with
- Java 17
- Maven
- Discord JDA
- SQLite

---

## 🧪 Getting Started
1. Download the files from Github
2. Create a Discord Bot
   - Go to https://discord.com/developers/applications
   - Select 'New Application'
   - Reveal and copy the token. Save this for later. Do this through the 'Bot' tab
   - Turn on toggles for:
     - Server Members Intent
     - Message Content Intent
   - Visit OAuth2 -> Select 'bot' -> Allow the bot to View Channels, Send Messages, Manage Messages, Read Message History, Use Slash Commands
     - Copy the generated URL and add the bot to your ACO server as well as your webhook server
3. Visit Discord
   - Webhook Server
     - Give the bot close to all permissions
   - ACO server
     - Create a webhook URL and save it for later
4. Open the '.env' file located within the downloaded fields. Here, make sure to paste the token, channel id being checked for webhooks, your server (guild) id, and webhook URL
5. Download Both Java and Maven from the links below and set them up
   - https://www.oracle.com/java/technologies/downloads/?er=221886
   - https://maven.apache.org/download.cgi
     - Select the Binary zip archive and install
     - After download, unzip the folder in your 'Program Files'
     - Open 'Environment Variables' (Search on Windows)
     - Click 'Edit System Environment Variables'
     - Set 'Maven Home'
       - Under System Variables, Click 'New'
         - Variable Name: Maven Home
         - Variable Value: C:\Program Files\Apache\Maven\apache-maven-3.9.6
     - Add Maven to Path
       - Find the Path variable under System Variables and select Edit
       - Click New -> Add
         - C:\Program Files\Apache\Maven\apache-maven-3.9.6\bin
     - Verify that Maven was installed by opening a Command Prompt and typing in 'mvn -v'
6. Almost there! Open your terminal now and cd into your project root folder, then run 'mvn clean package'
7. Your Jar file will be in the target/ folder. Copy the name.
8. Run 'java -jar target/yourproject-1.0-shaded.jar'
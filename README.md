# Online Connect 4 🔴🟡

A fully featured, multithreaded Online Connect 4 game built with Java and JavaFX. This project utilizes a client-server architecture via Java Sockets to allow multiple players to connect, create accounts, chat, and play Connect 4 over a network. 

## ✨ Key Features

* **Client-Server Architecture:** A central server capable of handling multiple concurrent client connections, matchmaking, and routing game logic.
* **Account System & Authentication:** Users can create new accounts or log into existing ones. 
* **Persistent Data Storage:** Player statistics (Games Played, Wins, Losses, Draws) and login credentials are saved locally on the server in a `data.txt` file, persisting across server restarts.
* **Real-time Matchmaking:** Players can queue up to find opponents. If no opponent is available, they will wait in a lobby until another player connects.
* **Live Chat:** An integrated text chat system allows players to communicate during a match.
* **Rematch System:** Post-game options allow players to immediately request a rematch or return to the main menu.
* **Server GUI:** An administrative server dashboard that monitors live server events, active connections, and user actions in real-time.

## 🛠️ Technologies Used

* **Java:** Core application logic, multithreading (`Thread`), and Collections (`HashMap`, `ArrayList`).
* **JavaFX:** Graphical User Interface (GUI) for both the Client application and the Server dashboard.
* **Java Sockets (`java.net.Socket` & `ServerSocket`):** TCP network communication between clients and the server.
* **Java IO (`java.io.Serializable`):** Object serialization for transmitting custom `Message` payloads across the network.

## 🚀 How to Run

**Prerequisites:** You will need the **Java Development Kit (JDK)** and **JavaFX** installed on your machine. Running this through an IDE like IntelliJ IDEA or Eclipse (with JavaFX configured) is recommended.

### 1. Start the Server (Crucial)
The server must be initialized before any clients can connect.
1. Run the `GuiServer.java` file.
2. The Server GUI will open, indicating the server is active on `localhost` (Port: `5555`).
3. *Note: Upon closing the server window, it will automatically write and save the latest user data to `data.txt`.*

### 2. Start the Clients
1. Run the `GuiClient.java` file to launch a client instance. (You can run multiple instances to simulate multiple players).
2. Use the login screen to **Create a New Account** or log in with an existing username/password.
3. Once in the main menu, click **Play** to enter the matchmaking queue.
4. When another client also clicks **Play**, the game will start!

## 🗂️ Architecture Overview

* **`GuiServer.java` & `Server.java`:** Manages the Server GUI and core server logic. Uses `ServerAcceptThread` to listen for new connections and spawns a `ClientThread` for every user. `GameThread` manages the actual Connect 4 board logic and turn validation.
* **`GuiClient.java` & `Client.java`:** Manages the Client GUI scenes (Login, Main Menu, Game Board) and handles asynchronous communication with the server.
* **`Message.java` & `MessageType.java`:** A custom serializable object and Enum used as the standard communication protocol between the client and server (handling Logins, Game Requests, Chat Text, and Turn data).
* **`User.java`:** A data model representing a player's credentials and lifetime statistics.

## 🔮 Future Improvements

* Implement password hashing/encryption before saving to `data.txt` to enhance security.
* Add a leaderboard system to rank players by win/loss ratio.
* Network configuration to allow connections outside of `localhost` (LAN/WAN support).

---

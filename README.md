# Networked 3-Card Poker

## Project Description
This is a Java-based networked implementation of a 3-Card Poker game. The project consists of two main components:
- **Server**: Handles the game logic and client connections.
- **Client**: Provides a graphical interface for users to play the game.

## Prerequisites
1. **Java Development Kit (JDK)**: Ensure JDK 8 or higher is installed on your system.
2. **Environment Variables**: Set `JAVA_HOME` and add the `bin` directory to your `PATH`.
3. **Apache Maven**: Install Apache Maven to build the project
   

## Running the Project
Follow these steps to set up and run the server and client.

### 1. Start the Server
1. Navigate to the `Proj3Server` directory:
   ```bash
   cd Networked-3-Card-Poker/Proj3Server
   ```
2. Compile the server code:
   ```bash
   mvn compile
   ```
  
3. Run the server:
   ```bash
   mvn exec:java
   ```
   The server will start and begin listening on a default port (e.g., `5000`).

### 2. Start the Client
1. Open a new terminal and navigate to the `Proj3Client` directory:
   ```bash
   cd Networked-3-Card-Poker/Proj3Client
   ```
2. Compile the client code:
   ```bash
   mvn compile
   ```
   
3. Run the client:
   ```bash
   mvn exec:java
   ```
4. When prompted, enter the server's IP address and the port number (default: `5000`).

### Note
Ensure the client and server are running on the same port.



package com.github.aboodRS.collaborative_markdown_editor_server;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import org.springframework.security.crypto.bcrypt.BCrypt;
import java.util.ArrayList;
import java.util.List;

public class MongoClientConnection {
    private MongoDatabase database; // Database instance to perform operations on
    private MongoCollection<Document> collection; // Collection instance for user data
    private MongoClient mongoClient;  // Hold the client instance for the whole class

    // Constructor to initialize the MongoDB connection
    public MongoClientConnection() {
        String connectionString = "mongodb+srv://aboodrs:CollaborativeMarkdownEditorPass@cluster0.hfwgk.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        
        // Initialize the MongoClient without auto-closing
        mongoClient = MongoClients.create(settings);
        try {
            // Send a ping to confirm a successful connection
            database = mongoClient.getDatabase("CollaborativeMarkdownEditor");
            database.runCommand(new Document("ping", 1));
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
            collection = database.getCollection("Users");
        } catch (MongoException e) {
            e.printStackTrace();
        }
    }

    // Create a user account with a hashed password
    // This method checks if a user already exists and creates a new account if not,
    // hashing the password for security before storing it in the database.
    public boolean createAccount(String username, String password) throws AccountCreationException {
        try {
            Document existingUser = collection.find(new Document("username", username)).first();
            if (existingUser != null) {
                System.err.println("Error: Username '" + username + "' already exists.");
                return false; // User already exists, return false
            }
            
            // Hash the password before storing it
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Create and insert a new user document
            Document document = new Document("username", username)
                    .append("password", hashedPassword)
                    .append("friendList", new ArrayList<>())
                    .append("sessionId", "");

            collection.insertOne(document);
            System.out.println("User created successfully!");
            return true; // Account created successfully, return true
        } catch (MongoException e) {
            // Throw a custom exception for other MongoDB errors
            throw new AccountCreationException("Error creating user: " + e.getMessage());
        }
    }

    // Close the MongoClient connection when you're done with it
    // This method ensures that resources are released properly when the connection is no longer needed.
     public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoClient connection closed.");
        }
    }

     // Log in a user by verifying their username and password
     // This method checks if the provided username exists and verifies the password,
     // returning appropriate status codes based on the result of the login attempt.
    public int loginUser(String username, String password) throws AccountCreationException {
        try {
            Document foundUser = collection.find(new Document("username", username)).first();
            
            if (foundUser != null) {
                String storedHashedPassword = foundUser.getString("password");
                
                // Compare the entered password with the stored hashed password
                if (BCrypt.checkpw(password, storedHashedPassword)) {
                    System.out.println("Login successful!");
                    return 1; // Login successful
                } else {
                    System.out.println("Incorrect password.");
                    return 0; // Incorrect password
                }
            } else {
                System.out.println("User not found.");
                return -1; // User not found
            }
        } catch (MongoException e) {
            // Throw a custom exception for MongoDB errors
            throw new AccountCreationException("Error during login: " + e.getMessage());
        }
    }
    
    // Add a friend to the logged-in user's friend list
    // This method checks if the friend exists and updates the logged-in user's friend list
    // if the friend is not already in the list.
    public String addFriend(String loggedInUserId, String friendUsername) throws AccountCreationException {
        try {
            // Find the logged-in user by their ID (assumed to be stored in loggedInUserId)
            Document loggedInUser = collection.find(new Document("username", loggedInUserId)).first();
            if (loggedInUser != null) {
                // Find the friend by username
                Document friendUser = collection.find(new Document("username", friendUsername)).first();
                if (friendUser != null) {
                    // Get the logged-in user's friend list
                    List<String> friendList = (List<String>) loggedInUser.get("friendList");
                    
                    // Check if the friend is already in the friend list
                    if (!friendList.contains(friendUsername)) {
                        // Add the friend's username to the logged-in user's friend list
                        friendList.add(friendUsername);
                        collection.updateOne(new Document("username", loggedInUserId), 
                                             new Document("$set", new Document("friendList", friendList)));
                        System.out.println("Friend added successfully!");
                        return "Friend added successfully!";

                    } else {
                        System.out.println("Friend already in the friend list.");
                        return "Friend already in the friend list.";
                    }
                } else {
                    System.out.println("Friend not found.");
                    return "Friend not found.";
                }
            } else {
                System.out.println("Logged-in user not found.");
                return "Logged-in user not found.";

            }
        } catch (MongoException e) {
            throw new AccountCreationException("Error adding friend: " + e.getMessage());
        }
    }
    
    // Add a session ID for the specified user
    // This method updates the session ID for a user, allowing the system to track 
    // active sessions for that user.
    public void addSessionId(String username, String sessionId) {
        try {
            // Update the user's sessionId field
            collection.updateOne(
                new Document("username", username),
                new Document("$set", new Document("sessionId", sessionId))
            );
            System.out.println("Session ID added successfully for user: " + username);
        } catch (MongoException e) {
            System.err.println("Error adding session ID: " + e.getMessage());
        }
    }

    // Remove the session ID for the specified user
    // This method clears the session ID for a user when they log out or when 
    // the session is no longer active, helping to maintain session integrity.
    public void removeSessionId(String username) {
        try {
            // Remove the sessionId field for the user
            collection.updateOne(
                new Document("username", username),
                new Document("$unset", new Document("sessionId", ""))
            );
            System.out.println("Session ID removed successfully for user: " + username);
        } catch (MongoException e) {
            System.err.println("Error removing session ID: " + e.getMessage());
        }
    }
    
    // Retrieve the friend list for the logged-in user
    // This method returns the list of friends for a specified user,
    // or an empty list if the user has no friends or is not found,
    // to allow the user to join a friend's session
    public List<String> getFriendList(String loggedInUserId) {
        try {
            // Find the logged-in user by their username
            Document loggedInUser = collection.find(new Document("username", loggedInUserId)).first();
            if (loggedInUser != null) {
                // Get the friend's list from the user document
                List<String> friendList = (List<String>) loggedInUser.get("friendList");
                return friendList != null ? friendList : new ArrayList<>(); // Return the friend list or an empty list if null
            } else {
                System.out.println("Logged-in user not found.");
            }
        } catch (MongoException e) {
            System.err.println("Error retrieving friend list: " + e.getMessage());
        }
        return new ArrayList<>(); // Return an empty list on error
    }
    
    // Join a session of a friend by checking mutual friendship and active session
    // This method allows a user to join their friend's active session if they 
    // are mutual friends and the friend has an active session.
    public String joinFriendSession(String loggedInUserId, String friendUsername) {
        try {
            // Find the logged-in user by username
            Document loggedInUser = collection.find(new Document("username", loggedInUserId)).first();
            if (loggedInUser != null) {
                // Find the friend by username
                Document friendUser = collection.find(new Document("username", friendUsername)).first();
                if (friendUser != null) {
                    // Get the friend lists of both users
                    List<String> loggedInUserFriendList = (List<String>) loggedInUser.get("friendList");
                    List<String> friendUserFriendList = (List<String>) friendUser.get("friendList");

                    // Check if both users have each other in their friend lists
                    if (loggedInUserFriendList != null && friendUserFriendList != null &&
                            loggedInUserFriendList.contains(friendUsername) &&
                            friendUserFriendList.contains(loggedInUserId)) {

                        // Check if the friend has an active session
                        String friendSessionId = friendUser.getString("sessionId");
                        if (friendSessionId != null && !friendSessionId.isEmpty()) {
                            System.out.println("Both users are friends. Joining friend's session: " + friendSessionId);
                            return friendSessionId; // Return the friend's session ID for joining
                        } else {
                            System.out.println("Friend does not have an active session.");
                            return "Friend does not have an active session.";
                        }
                    } else {
                        System.out.println("Both users must have each other as friends.");
                        return "The User you are trying to join does not have you added as a Friend. Both users must have each other as friends to join a friends session.";
                    }
                } else {
                    System.out.println("Friend not found.");
                    return "Friend not found.";
                }
            } else {
                System.out.println("Logged-in user not found.");
                return "Logged-in user not found.";
            }
        } catch (MongoException e) {
            System.err.println("Error joining friend's session: " + e.getMessage());
            return "Error joining friend's session: " + e.getMessage();
        }
    }

}

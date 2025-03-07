package com.github.aboodRS.collaborative_markdown_editor_server;

import javax.swing.*;
import java.awt.*;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;
import java.util.List;
import java.util.Stack;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.event.ActionEvent;

public class MarkdownEditorApp extends JFrame {
    private static final long serialVersionUID = 1L;

    private MarkdownEditor markdownEditor;
    private MarkdownPreview markdownPreview;
    private MarkdownWebSocketClient webSocketClient;
    private boolean isRemoteUpdate = false;
    private boolean onlineMode = false;
    private AtomicBoolean serverRunning = new AtomicBoolean(false);
    private String sessionIdText;
    private String lastBroadcastedMarkdown = "";
    MongoClientConnection myclient = new MongoClientConnection();
    private String loggedInUserId; // To store the logged-in user's ID
    private Stack<String> undoStack;
    private Stack<String> redoStack;
    int k = 1;
    
    /**
     *  The constructor MarkdownEditorApp is called whenever you start the program which creates
     *  a new empty undo and redo stack in addition to setting up the keybinding for the the undo
     *  and redo functions (Ctrl Z).
     */
    public MarkdownEditorApp() {
        initializeUI();
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        setupKeyBindings();
    }

    /**
     * Sets up the main UI for the Collaborative Markdown Editor, including 
     * menus for file, edit, session, and account management, as well as the 
     * editor and preview components. It also assigns key action listeners.
     */
    private void initializeUI() {
        // Sets up the main window properties, including title, size, and default close operation.
    	setTitle("Collaborative Markdown Editor");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));

        // Creates and adds the markdown editor and preview components side-by-side.
        markdownEditor = new MarkdownEditor();
        markdownPreview = new MarkdownPreview();
        add(markdownEditor);
        add(markdownPreview);
        
        // Initializes the menu bar, which holds the application’s main menu options.
        JMenuBar menuBar = new JMenuBar();
        
        // File menu: holds options to create, open, and save files in the editor.
        JMenu fileMenu = new JMenu("File");
        JMenuItem newFileMenuItem = new JMenuItem("New File");
        JMenuItem openFileMenuItem = new JMenuItem("Open File");
        JMenuItem saveFileMenuItem = new JMenuItem("Save File");
        fileMenu.add(newFileMenuItem);
        fileMenu.add(openFileMenuItem);
        fileMenu.add(saveFileMenuItem);
        menuBar.add(fileMenu);

        // Edit menu: adds undo and redo actions for editing functionality.
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoMenuItem = new JMenuItem("Undo");
        JMenuItem redoMenuItem = new JMenuItem("Redo");
        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);
        menuBar.add(editMenu);
        
        // Account menu: allows account-related actions, such as creating an account, logging in, and managing friends.
        JMenu accountMenu = new JMenu("Account");
        JMenuItem createAccountMenuItem = new JMenuItem("Create Account");
        JMenuItem loginMenuItem = new JMenuItem("Login");
        JMenuItem addFriendMenuItem = new JMenuItem("Add Friend");
        JMenuItem friendListMenuItem = new JMenuItem("Friend List"); // New menu item for friend list

        
        // Update Preview button for manual update of the markdown preview pane.
        JMenuItem updatePreviewButton = new JMenuItem("Update Preview");

        
        // Adds account-related actions to the account menu.
        accountMenu.add(createAccountMenuItem);
        accountMenu.add(loginMenuItem);
        accountMenu.add(addFriendMenuItem);
        accountMenu.add(friendListMenuItem); // Add to the menu

        // Adds the account menu to the menu bar.
        menuBar.add(accountMenu);
        
        // Session menu: allows creating, joining, and leaving collaborative sessions.
        JMenu sessionMenu = new JMenu("Session");
        JMenuItem createSessionMenuItem = new JMenuItem("Create Session");
        JMenuItem joinSessionMenuItem = new JMenuItem("Join Session");
        JMenuItem leaveSessionMenuItem = new JMenuItem("Leave Session");

        sessionMenu.add(createSessionMenuItem);
        sessionMenu.add(joinSessionMenuItem);
        sessionMenu.add(leaveSessionMenuItem);
        menuBar.add(sessionMenu);
        
        // Insert menu: provides options for inserting common Markdown elements like tables, links, images, etc.
        JMenu insertMenu = new JMenu("Insert");
        JMenuItem tableMenuItem = new JMenuItem("Table");
        JMenuItem linkMenuItem = new JMenuItem("Link");
        JMenuItem imageMenuItem = new JMenuItem("Image");
        JMenuItem codeBlockMenuItem = new JMenuItem("Code Block");
        JMenuItem inlineCodeMenuItem = new JMenuItem("Inline Code");
        JMenuItem orderedListMenuItem = new JMenuItem("Ordered List");
        JMenuItem unorderedListMenuItem = new JMenuItem("Unordered List");

        // Adds action listeners for inserting elements to streamline content creation in the editor.
        tableMenuItem.addActionListener(e -> insertTable());
        linkMenuItem.addActionListener(e -> insertLink());
        imageMenuItem.addActionListener(e -> insertImage());
        codeBlockMenuItem.addActionListener(e -> insertCodeBlock());
        inlineCodeMenuItem.addActionListener(e -> insertInlineCode());
        orderedListMenuItem.addActionListener(e -> insertOrderedList());
        unorderedListMenuItem.addActionListener(e -> insertUnorderedList());

        // Adds all insert options to the insert menu.
        insertMenu.add(tableMenuItem);
        insertMenu.add(linkMenuItem);
        insertMenu.add(imageMenuItem);
        insertMenu.add(codeBlockMenuItem);
        insertMenu.add(inlineCodeMenuItem);
        insertMenu.add(orderedListMenuItem);
        insertMenu.add(unorderedListMenuItem);
        menuBar.add(insertMenu);
        menuBar.add(updatePreviewButton);
        
        // Sets the menu bar for the main window.
        setJMenuBar(menuBar);

        // Action listener for new file creation, which resets the editor content.
        newFileMenuItem.addActionListener(e -> newFile());
        
        // Action listener for opening a file, which loads content into the editor.
        openFileMenuItem.addActionListener(e -> openFile());
        
        // Action listener for saving the current file content from the editor.
        saveFileMenuItem.addActionListener(e -> saveFile());

        // Action listeners for editing operations like undo and redo.
        undoMenuItem.addActionListener(e -> undo());
        redoMenuItem.addActionListener(e -> redo());
        
        // Text fields for username and password for login and account creation dialogs.
        JTextField usernameField = new JTextField(10);
        JPasswordField passwordField = new JPasswordField(10);

        // Action listener for creating account
        createAccountMenuItem.addActionListener(e -> createAccountDialog(usernameField, passwordField));

        // Action listener for logging in
        loginMenuItem.addActionListener(e -> loginDialog(usernameField, passwordField));

        // Action listener for adding friends
        addFriendMenuItem.addActionListener(e -> addFriendDialog());
        
        // Action listener for viewing friend list
        friendListMenuItem.addActionListener(e -> showFriendListDialog());

        // Action listener for creating a session.
        createSessionMenuItem.addActionListener(e -> createSession());
        
        // Action listener for joining a session.
        joinSessionMenuItem.addActionListener(e -> joinSession(JOptionPane.showInputDialog(this, "Enter Session ID:")));
        
        // Action listener for leaving a session.
        leaveSessionMenuItem.addActionListener(e -> leaveSession());
        
        // Listens for text changes in the markdown editor to broadcast updates to other session members if needed.
        markdownEditor.addTextChangeListener(markdownText -> {
            if (!isRemoteUpdate) {
                // Only broadcasts text changes if it's not from a remote update to avoid loops.
            	broadcastMarkdownText(markdownText);
            }
        });
        
        // Action listener for manually updating the preview pane with the editor's current content.
        updatePreviewButton.addActionListener(e -> {
            // Get the current content from the markdown editor and updates the preview panel.
            String markdownText = markdownEditor.getMarkdownPane().getText();
            
            markdownPreview.updatePreview(markdownText);
        });
        
        // Makes the UI visible after initialization.
        setVisible(true);
    }

 // Creates a new, blank document in the editor by clearing content and reset undo/redo stacks
    private void newFile() {
        markdownEditor.getMarkdownPane().setText(""); // Clear the editor
        lastBroadcastedMarkdown = ""; // Reset last broadcasted markdown
        markdownPreview.updatePreview(""); // Clear the preview
        undoStack.clear(); // Clear the undo stack
        redoStack.clear(); // Clear the redo stack
    }
 // Opens a markdown file and loads its content into the editor
    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                markdownEditor.getMarkdownPane().setText(content.toString());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error reading file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
 // Saves the current content of the editor to a markdown file
    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
                writer.write(markdownEditor.getMarkdownPane().getText());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
 // Reverts the editor to the previous state by popping from the undo stack
    private void undo() {
        if (undoStack.size() > 1) {
            k = 0;
            // Push current text to redo stack before undo
            redoStack.push(markdownEditor.getMarkdownPane().getText());
            String secondLastText = undoStack.pop();
            markdownEditor.getMarkdownPane().setText(secondLastText);
            undoStack.pop();
        } else {
            JOptionPane.showMessageDialog(this, "No actions to undo.", "Undo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

 // Re-applies a previously undone change by popping from the redo stack
    private void redo() {
        if (!redoStack.isEmpty()) {
            // Push current text to undo stack before redo
            undoStack.push(markdownEditor.getMarkdownPane().getText());
            String lastRedoText = redoStack.pop();
            markdownEditor.getMarkdownPane().setText(lastRedoText);
        } else {
            JOptionPane.showMessageDialog(this, "No actions to redo.", "Redo", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
 // Opens a dialog for creating a new account with username and password fields
    private void createAccountDialog(JTextField usernameField, JPasswordField passwordField) {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Create Account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            createAccount(usernameField.getText(), new String(passwordField.getPassword()));
        }
    }

 // Opens a login dialog for entering a username and password
    private void loginDialog(JTextField usernameField, JPasswordField passwordField) {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            login(usernameField.getText(), new String(passwordField.getPassword()));
        	loggedInUserId = usernameField.getText();
        }
    }

 // Handles account creation and displays success or error messages
    private void createAccount(String username, String password) {
        try {
            boolean accountCreated = myclient.createAccount(username, password);

            if (accountCreated) {
                JOptionPane.showMessageDialog(this, "Account created! Username: " + username, "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error: Username '" + username + "' already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (AccountCreationException e) {
            JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

 // Handles user login and displays messages based on login results
    private void login(String username, String password) {
        try {
            int loginResult = myclient.loginUser(username, password);

            switch (loginResult) {
                case 1: // Login successful
                    JOptionPane.showMessageDialog(this, "Login successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case 0: // Incorrect password
                    JOptionPane.showMessageDialog(this, "Incorrect password. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    break;
                case -1: // User not found
                    JOptionPane.showMessageDialog(this, "User not found. Please check your username.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        } catch (AccountCreationException e) {
            // Handle database errors
            JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

 // Opens a dialog to add a friend by username, showing feedback for success or errors
    private void addFriendDialog() {
        try {
        	if (loggedInUserId == null) {
        		JOptionPane.showMessageDialog(this, "You must be logged in to add a friend.", "Error", JOptionPane.ERROR_MESSAGE);
        		return;
        	}

        	String friendUsername = JOptionPane.showInputDialog(this, "Enter the Username of your friend:");
        	if (friendUsername != null && !friendUsername.trim().isEmpty()) {
        		String check = myclient.addFriend(loggedInUserId, friendUsername); // Implement this method in MongoClientConnection
        		switch (check) {
        		case "Friend added successfully!": // Login successful
        			JOptionPane.showMessageDialog(this, friendUsername + " added to your Friendslist", "Success", JOptionPane.INFORMATION_MESSAGE);
        			break;
        		case "Friend already in the friend list.": // Incorrect password
        			JOptionPane.showMessageDialog(this, friendUsername + " is already in your Friendslist", "Error", JOptionPane.ERROR_MESSAGE);
        			break;
        		case "Friend not found.": // User not found
        			JOptionPane.showMessageDialog(this, "User not found. Make sure the Username you entered is correct.", "Error", JOptionPane.ERROR_MESSAGE);
        			break;
        		}
        	}
        }catch (AccountCreationException e) {
        			// Handle database errors
        			JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        		}
    	}

    
 // Shows the friend list in a dialog, with buttons to join a friend's session
    private void showFriendListDialog() {
        if (loggedInUserId == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to view your friend list.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> friends = myclient.getFriendList(loggedInUserId); // Fetch the friend list
        JPanel panel = new JPanel(new GridLayout(friends.size(), 2)); // Create panel to display friends
        for (String friend : friends) {
            JButton joinButton = new JButton("Join Session");
            joinButton.addActionListener(e -> joinSession(myclient.joinFriendSession(loggedInUserId, friend))); // Join session for the friend
            panel.add(new JLabel(friend)); // Display friend's name
            panel.add(joinButton); // Add join button
        }
        
        JOptionPane.showMessageDialog(this, panel, "Friend List", JOptionPane.PLAIN_MESSAGE);
    }
    
 // Creates a collaborative editing session with a password, sharing session ID with users
    private void createSession() {
        if (serverRunning.get()) {
            JOptionPane.showMessageDialog(this, "Session already created.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            // Prompt the user to enter a password to secure the session
            JPasswordField passwordField = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(this, passwordField, "Enter password for the session:",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            // If the user cancels, exit the method
            if (option != JOptionPane.OK_OPTION) {
                return;
            }

            // Retrieve password as a String
            String password = new String(passwordField.getPassword());
            
            // Generate a unique session ID and initialize the WebSocket connection for collaboration
            String sessionId = UUID.randomUUID().toString();
            webSocketClient = new MarkdownWebSocketClient("ws://collaborativemarkdowneditor.onrender.com/collaborate/" + sessionId);
            
         // Set handler to process incoming messages
            webSocketClient.setMessageHandler(this::onWebSocketMessage);
            onlineMode = true;
            sessionIdText = sessionId; // Save session ID for display or future reference
            markdownEditor.getMarkdownPane().setText(sessionIdText);
            markdownPreview.updatePreview(sessionIdText);
            
            // Store session ID in the database for the logged-in user, if available
            if (loggedInUserId != null) {
                myclient.addSessionId(loggedInUserId, sessionId);
            }
            
            // Send password to secure the session
            webSocketClient.send("setPassword:" + password);
            JOptionPane.showMessageDialog(this, "Session created! Session ID: " + sessionId, "Session Created", JOptionPane.INFORMATION_MESSAGE);
            serverRunning.set(true); // Indicate that a session is now running
        } catch (Exception e) {
            e.printStackTrace();
            onlineMode = false;
            JOptionPane.showMessageDialog(this, "Failed to create session.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void joinSession(String sessionLink) {
        // Prevent joining a new session if already connected to another session
        if (onlineMode) {
            JOptionPane.showMessageDialog(this, "Already connected to a session.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            // Prompt the user to enter a password to join the session
            JPasswordField passwordField = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(this, passwordField, "Enter password for the session:",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            // Exit if the user cancels the join operation
            if (option != JOptionPane.OK_OPTION) {
                return;
            }

            // Retrieve password as a String
            String password = new String(passwordField.getPassword());
            
            // Connect to the specified session using WebSocket and set a message handler for collaboration
            webSocketClient = new MarkdownWebSocketClient("ws://collaborativemarkdowneditor.onrender.com/collaborate/" + sessionLink);
            webSocketClient.setMessageHandler(this::onWebSocketMessage);
            onlineMode = true;
            sessionIdText = sessionLink; // Store the session ID
            webSocketClient.send("join:" + password); // Send join request with password to authenticate
            JOptionPane.showMessageDialog(this, "Attempting to connect to session: " + sessionLink, "Connecting", JOptionPane.INFORMATION_MESSAGE);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            onlineMode = false;
            JOptionPane.showMessageDialog(this, "Failed to connect to session.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable e) {
            e.printStackTrace();
            onlineMode = false;
            // Handle specific errors related to friendship and session access restrictions
            if (e.getMessage().contains("The User you are trying to join does not have you added as a Friend. Both users must have each other as friends to join a friends session.")) {
                JOptionPane.showMessageDialog(this, "The User you are trying to join does not have you added as a Friend. Both users must have each other as friends to join a friends session.", "Error", JOptionPane.ERROR_MESSAGE);
            }else if (e.getMessage().contains("Friend not found.")) {
                JOptionPane.showMessageDialog(this, "Friend not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }else if (e.getMessage().contains("Friend does not have an active session.")) {
                JOptionPane.showMessageDialog(this, "Friend does not have an active session.", "Error", JOptionPane.ERROR_MESSAGE);
            }else {
            	JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            	}
        }
    }

    private void leaveSession() {
        // Check if a session is currently active
        if (!onlineMode) {
            JOptionPane.showMessageDialog(this, "You are not in a session.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Remove session ID from the database whenever you leave the session so that if someone tries to join, it wont redirect them to a wrong session.
        if (loggedInUserId != null) {
        	myclient.removeSessionId(loggedInUserId);
        }
        
        // Attempt to close the WebSocket connection, ending the session
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
                webSocketClient = null; // Reset client to allow future sessions
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to leave the session properly.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        // Reset flags to reflect that no session is active
        onlineMode = false;
        serverRunning.set(false); 
        JOptionPane.showMessageDialog(this, "You have left the session.", "Session Left", JOptionPane.INFORMATION_MESSAGE);
    }
    
 // Handles incoming messages from the WebSocket and updates the editor if not a remote update
    private void onWebSocketMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Received message: " + message);
            if (message.startsWith("SYSTEM:")) {
                // Extract the actual system message by removing the "SYSTEM:" prefix
                String systemMessage = message.substring("SYSTEM:".length()).trim();
                JOptionPane.showMessageDialog(this, "Incorrect password. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                onlineMode = false; // Optionally reset onlineMode
            }
            if (!isRemoteUpdate) {
                isRemoteUpdate = true;
                markdownEditor.getMarkdownPane().setText(message);
                //markdownPreview.updatePreview(message);
                isRemoteUpdate = false;
            }
        });
    }

 // Sends the current markdown text to other session users via WebSocket, updating undo stack for both When in a session or alone.
    private void broadcastMarkdownText(String markdownText) {
        if (onlineMode && !markdownText.equals(lastBroadcastedMarkdown)) {
            try {
                webSocketClient.send(markdownText);
                undoStack.push(lastBroadcastedMarkdown);
                redoStack.clear(); // Clear redo stack on new input
                int numberOfElements = undoStack.size();
                System.out.println("Number of elements in undoStack: " + numberOfElements);
                lastBroadcastedMarkdown = markdownText;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (!onlineMode && !markdownText.equals(lastBroadcastedMarkdown)) {
            try {
            	if (k == 1) {
            		undoStack.push(lastBroadcastedMarkdown);
                	lastBroadcastedMarkdown = markdownText;
            	}
            	else {
            		k = 1;
                	lastBroadcastedMarkdown = markdownText;
            	}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

 // Sets up keyboard shortcuts for undo (Ctrl+Z) and redo (Ctrl+Y) actions in the editor
    private void setupKeyBindings() {
        // Get the input map and action map for the markdown pane
        InputMap inputMap = markdownEditor.getMarkdownPane().getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = markdownEditor.getMarkdownPane().getActionMap();

        // Bind Ctrl + Z to the undo action
        inputMap.put(KeyStroke.getKeyStroke("control Z"), "undoAction");
        actionMap.put("undoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        // Bind Ctrl + Y to the redo action
        inputMap.put(KeyStroke.getKeyStroke("control Y"), "redoAction");
        actionMap.put("redoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });
    }
    
 // Insert methods
    private void insertTable() {
        // Code to insert a markdown table template
        String tableTemplate = "| Column 1 | Column 2 |\n|-----------|-----------|\n|           |           |\n";
        String currentText = markdownEditor.getMarkdownPane().getText();
        markdownEditor.getMarkdownPane().setText(currentText + tableTemplate);
    }

    private void insertLink() {
        // Code to insert a markdown link template
        String linkTemplate = "[Link Text](http://example.com)\n";
        String currentText = markdownEditor.getMarkdownPane().getText();
        markdownEditor.getMarkdownPane().setText(currentText + linkTemplate);
    }

    private void insertImage() {
        // Code to insert a markdown image template
        String imageTemplate = "![Alt Text](http://example.com/image.jpg)\n";
        String currentText = markdownEditor.getMarkdownPane().getText();
        markdownEditor.getMarkdownPane().setText(currentText + imageTemplate);
    }

    private void insertCodeBlock() {
        // Code to insert a markdown code block template
        String codeBlockTemplate = "```\nCode here\n```\n";
        String currentText = markdownEditor.getMarkdownPane().getText();
        markdownEditor.getMarkdownPane().setText(currentText + codeBlockTemplate);
    }

    private void insertInlineCode() {
        // Code to insert markdown inline code template
        String inlineCodeTemplate = "`Inline Code`";
        String currentText = markdownEditor.getMarkdownPane().getText();
        markdownEditor.getMarkdownPane().setText(currentText + inlineCodeTemplate);
    }

    private void insertOrderedList() {
        // Code to insert a markdown ordered list template
        String orderedListTemplate = "1. Item 1\n2. Item 2\n3. Item 3\n";
        String currentText = markdownEditor.getMarkdownPane().getText();
        markdownEditor.getMarkdownPane().setText(currentText + orderedListTemplate);
    }

    private void insertUnorderedList() {
        // Code to insert a markdown unordered list template
        String unorderedListTemplate = "- Item 1\n- Item 2\n- Item 3\n";
        String currentText = markdownEditor.getMarkdownPane().getText();
        markdownEditor.getMarkdownPane().setText(currentText + unorderedListTemplate);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MarkdownEditorApp::new);
    }
}

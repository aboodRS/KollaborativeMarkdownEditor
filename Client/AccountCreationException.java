package com.github.aboodRS.collaborative_markdown_editor_server;

//Custom exception class for handling errors during account creation
//This exception is used to provide specific error messages related to account 
//creation processes, allowing for better error handling and debugging
//when dealing with user account operations, such as creating accounts or logging in.
public class AccountCreationException extends Exception {
    public AccountCreationException(String message) {
        super(message);
    }
}

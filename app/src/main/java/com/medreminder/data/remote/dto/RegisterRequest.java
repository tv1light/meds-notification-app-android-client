package com.medreminder.data.remote.dto;

public class RegisterRequest {
    public String login;
    public String password;

    public RegisterRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }
}

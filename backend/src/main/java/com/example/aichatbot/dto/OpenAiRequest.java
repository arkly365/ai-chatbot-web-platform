package com.example.aichatbot.dto;
public class OpenAiRequest {

    private String model;
    private String input;

    public OpenAiRequest() {
    }

    public OpenAiRequest(String model, String input) {
        this.model = model;
        this.input = input;
    }

    public String getModel() {
        return model;
    }

    public String getInput() {
        return input;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
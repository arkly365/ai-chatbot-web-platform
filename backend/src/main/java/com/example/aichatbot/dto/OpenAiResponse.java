package com.example.aichatbot.dto;
import java.util.List;

public class OpenAiResponse {

    private List<Output> output;

    public List<Output> getOutput() {
        return output;
    }

    public void setOutput(List<Output> output) {
        this.output = output;
    }

    public static class Output {
        private List<Content> content;

        public List<Content> getContent() {
            return content;
        }

        public void setContent(List<Content> content) {
            this.content = content;
        }
    }

    public static class Content {
        private String type;
        private String text;

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
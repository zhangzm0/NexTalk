package com.techstar.nexchat.api;

import java.util.List;
import java.util.ArrayList;

public class ChatRequest {
    public String model = "moonshot-v1-128k";   // 默认，后面可改
    public List<Message> messages = new ArrayList<Message>();

    public static class Message {
        public String role;
        public String content;
        public Message(String role, String content){
            this.role    = role;
            this.content = content;
        }
    }
}

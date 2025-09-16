package com.techstar.nexchat.api;

import java.util.List;

public class ChatResponse {
    public List<Choice> choices;

    public static class Choice {
        public Message message;
        public static class Message {
            public String role;
            public String content;
        }
    }

    /* 兜底字段，防空 */
    public String getReply() {
        if (choices == null || choices.isEmpty()) return "";
        Choice first = choices.get(0);
        if (first == null || first.message == null) return "";
        return first.message.content == null ? "" : first.message.content;
    }
}

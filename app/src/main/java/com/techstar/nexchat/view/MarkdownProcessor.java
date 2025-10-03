package com.techstar.nexchat.view;

import android.content.Context;
import android.text.SpannableString;
import com.techstar.nexchat.util.MarkdownParser;

public class MarkdownProcessor {
    private static MarkdownProcessor instance;
    private MarkdownParser parser;

    private MarkdownProcessor(Context context) {
        parser = new MarkdownParser();
    }

    public static synchronized MarkdownProcessor getInstance(Context context) {
        if (instance == null) {
            instance = new MarkdownProcessor(context);
        }
        return instance;
    }

    public SpannableString process(String text) {
        if (text == null) {
            return new SpannableString("");
        }

        try {
            return parser.parse(text);
        } catch (Exception e) {
            // 如果解析失败，返回原始文本
            return new SpannableString(text);
        }
    }

    public SpannableString processSimple(String text) {
        return parser.parse(text);
    }
}

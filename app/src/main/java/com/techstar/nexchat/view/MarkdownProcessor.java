package com.techstar.nexchat.view;

import android.content.Context;
import android.text.SpannableString;
import com.techstar.nexchat.util.MarkdownParser;

public class MarkdownProcessor {
    private static MarkdownProcessor instance;
    private MarkdownParser parser;
    
    private MarkdownProcessor(Context context) {
        parser = new MarkdownParser();
        // 可以根据主题设置颜色
        // parser.setCodeBackgroundColor(ContextCompat.getColor(context, R.color.code_bg));
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
        
        // 检查是否包含引用
        if (text.contains("> ")) {
            return parser.parseWithQuotes(text);
        } else {
            return parser.parse(text);
        }
    }
    
    public SpannableString processSimple(String text) {
        return parser.parse(text);
    }
    
    // 设置自定义配置
    public void setCodeBackgroundColor(int color) {
        parser.setCodeBackgroundColor(color);
    }
    
    public void setCodeTextColor(int color) {
        parser.setCodeTextColor(color);
    }
    
    public void setLinkColor(int color) {
        parser.setLinkColor(color);
    }
}
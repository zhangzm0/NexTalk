package com.techstar.nexchat.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.graphics.Color;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MarkdownParser {
    
    // 正则表达式模式 - 预编译提高性能
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(.*?)~~");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`(.*?)`");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
    
    // 颜色配置
    private int codeBackgroundColor = Color.parseColor("#2D2D2D");
    private int codeTextColor = Color.parseColor("#E0E0E0");
    private int linkColor = Color.parseColor("#2196F3");
    private int quoteColor = Color.parseColor("#666666");
    
    public MarkdownParser() {}
    
    // 设置自定义颜色
    public void setCodeBackgroundColor(int color) {
        this.codeBackgroundColor = color;
    }
    
    public void setCodeTextColor(int color) {
        this.codeTextColor = color;
    }
    
    public void setLinkColor(int color) {
        this.linkColor = color;
    }
    
    public void setQuoteColor(int color) {
        this.quoteColor = color;
    }
    
    public SpannableString parse(String text) {
        if (text == null || text.isEmpty()) {
            return new SpannableString("");
        }
        
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        
        // 按处理顺序应用样式（重要：从内到外，从特殊到一般）
        processStrikethrough(builder);
        processBoldAndItalic(builder);
        processInlineCode(builder);
        processLinks(builder);
        
        return new SpannableString(builder);
    }
    
    private void processStrikethrough(SpannableStringBuilder builder) {
        Matcher matcher = STRIKETHROUGH_PATTERN.matcher(builder);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String content = matcher.group(1);
            
            // 移除标记符号
            builder.replace(start, end, content);
            
            // 应用删除线样式
            StrikethroughSpan span = new StrikethroughSpan();
            builder.setSpan(span, start, start + content.length(), 
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 重新匹配，因为文本长度改变了
            matcher = STRIKETHROUGH_PATTERN.matcher(builder);
        }
    }
    
    private void processBoldAndItalic(SpannableStringBuilder builder) {
        // 先处理粗体
        Matcher boldMatcher = BOLD_PATTERN.matcher(builder);
        while (boldMatcher.find()) {
            int start = boldMatcher.start();
            int end = boldMatcher.end();
            String content = boldMatcher.group(1);
            
            builder.replace(start, end, content);
            
            StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
            builder.setSpan(boldSpan, start, start + content.length(), 
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            boldMatcher = BOLD_PATTERN.matcher(builder);
        }
        
        // 再处理斜体（避免粗斜体冲突）
        Matcher italicMatcher = ITALIC_PATTERN.matcher(builder);
        while (italicMatcher.find()) {
            int start = italicMatcher.start();
            int end = italicMatcher.end();
            String content = italicMatcher.group(1);
            
            // 检查是否已经是粗体（避免重复应用）
            StyleSpan[] existingSpans = builder.getSpans(start, end, StyleSpan.class);
            boolean hasBold = false;
            for (StyleSpan span : existingSpans) {
                if (span.getStyle() == android.graphics.Typeface.BOLD) {
                    hasBold = true;
                    break;
                }
            }
            
            builder.replace(start, end, content);
            
            if (hasBold) {
                // 粗斜体
                StyleSpan boldItalicSpan = new StyleSpan(android.graphics.Typeface.BOLD_ITALIC);
                builder.setSpan(boldItalicSpan, start, start + content.length(), 
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // 纯斜体
                StyleSpan italicSpan = new StyleSpan(android.graphics.Typeface.ITALIC);
                builder.setSpan(italicSpan, start, start + content.length(), 
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            italicMatcher = ITALIC_PATTERN.matcher(builder);
        }
    }
    
    private void processInlineCode(SpannableStringBuilder builder) {
        Matcher matcher = INLINE_CODE_PATTERN.matcher(builder);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String content = matcher.group(1);
            
            builder.replace(start, end, content);
            
            int contentStart = start;
            int contentEnd = start + content.length();
            
            // 应用等宽字体
            TypefaceSpan monoSpaceSpan = new TypefaceSpan("monospace");
            builder.setSpan(monoSpaceSpan, contentStart, contentEnd, 
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 应用背景色
            BackgroundColorSpan bgSpan = new BackgroundColorSpan(codeBackgroundColor);
            builder.setSpan(bgSpan, contentStart, contentEnd, 
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 应用文字颜色
            ForegroundColorSpan textSpan = new ForegroundColorSpan(codeTextColor);
            builder.setSpan(textSpan, contentStart, contentEnd, 
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            matcher = INLINE_CODE_PATTERN.matcher(builder);
        }
    }
    
    private void processLinks(SpannableStringBuilder builder) {
        Matcher matcher = LINK_PATTERN.matcher(builder);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String linkText = matcher.group(1);
            String url = matcher.group(2);
            
            // 替换为链接文本
            builder.replace(start, end, linkText);
            
            int linkStart = start;
            int linkEnd = start + linkText.length();
            
            // 应用链接样式
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(linkColor);
            builder.setSpan(colorSpan, linkStart, linkEnd, 
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            UnderlineSpan underlineSpan = new UnderlineSpan();
            builder.setSpan(underlineSpan, linkStart, linkEnd, 
                           Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 存储 URL 数据（可用于点击事件）
            // 这里可以扩展为自定义 Span 来存储 URL
            
            matcher = LINK_PATTERN.matcher(builder);
        }
    }
    
    // 简单的引用处理（在行首的 >）
    public SpannableString parseWithQuotes(String text) {
        if (text == null || text.isEmpty()) {
            return new SpannableString("");
        }
        
        String[] lines = text.split("\n");
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        for (String line : lines) {
            if (line.startsWith("> ")) {
                // 引用行
                String quoteContent = line.substring(2);
                SpannableString quoteSpan = new SpannableString(quoteContent);
                quoteSpan.setSpan(new ForegroundColorSpan(quoteColor), 
                                0, quoteContent.length(), 
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(quoteSpan);
            } else {
                // 普通行
                builder.append(parse(line));
            }
            builder.append("\n");
        }
        
        // 移除最后一个换行符
        if (builder.length() > 0) {
            builder.delete(builder.length() - 1, builder.length());
        }
        
        return new SpannableString(builder);
    }
}
package com.techstar.nexchat.view;

import android.content.Context;
import android.text.SpannableString;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class MarkdownTextView extends AppCompatTextView {
    private MarkdownProcessor processor;
    
    public MarkdownTextView(Context context) {
        super(context);
        init(context);
    }
    
    public MarkdownTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public MarkdownTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        processor = MarkdownProcessor.getInstance(context);
    }
    
    @Override
    public void setText(CharSequence text, BufferType type) {
        if (text != null) {
            SpannableString formattedText = processor.process(text.toString());
            super.setText(formattedText, BufferType.SPANNABLE);
        } else {
            super.setText(text, type);
        }
    }
    
    // 直接设置原始文本（不处理 Markdown）
    public void setRawText(CharSequence text) {
        super.setText(text, BufferType.NORMAL);
    }
    
    // 设置 Markdown 文本
    public void setMarkdownText(String markdownText) {
        setText(markdownText);
    }
}
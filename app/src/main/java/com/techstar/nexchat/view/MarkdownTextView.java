package com.techstar.nexchat.view;

import android.content.Context;
import android.text.SpannableString;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;
import com.techstar.nexchat.util.FileLogger;

public class MarkdownTextView extends AppCompatTextView {
    private static final String TAG = "MarkdownTextView";
    private MarkdownProcessor processor;
    private FileLogger logger;

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
        try {
            logger = FileLogger.getInstance(context);
            processor = MarkdownProcessor.getInstance(context);
            logger.d(TAG, "MarkdownTextView initialized");
        } catch (Exception e) {
            // 如果初始化失败，降级为普通 TextView
            if (logger != null) {
                logger.e(TAG, "Failed to initialize MarkdownTextView", e);
            }
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (text != null && processor != null) {
            try {
                SpannableString formattedText = processor.process(text.toString());
                super.setText(formattedText, BufferType.SPANNABLE);
                return;
            } catch (Exception e) {
                if (logger != null) {
                    logger.e(TAG, "Failed to process markdown text", e);
                }
            }
        }
        // 降级处理：使用普通文本
        super.setText(text, type);
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

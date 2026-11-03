package com.xio.xcodestudio;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter implements TextWatcher {
    private final android.widget.EditText editor;
    private boolean applying;
    private static final Pattern TOKEN = Pattern.compile("//[^\\n]*|/\\*[\\s\\S]*?\\*/|#[^\\n]*|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\\b(?:public|private|protected|static|final|class|interface|enum|void|int|long|float|double|boolean|new|return|if|else|for|while|switch|case|break|continue|try|catch|throw|throws|import|package|extends|implements|const|let|var|function|async|await|def|lambda|from|as|pass|self|true|false|null|None|this|super|fun|val|var|object|when|println|struct|include|using|namespace|auto|template)\\b|\\b\\d+(?:\\.\\d+)?\\b");
    public SyntaxHighlighter(android.widget.EditText editor) { this.editor = editor; }
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    @Override public void afterTextChanged(Editable editable) {
        if (applying) return;
        // Avoid repeated full-buffer regex work on very large source files.
        if (editable.length() > 250_000) return;
        applying = true;
        try {
            ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) editable.removeSpan(span);
            StyleSpan[] styles = editable.getSpans(0, editable.length(), StyleSpan.class);
            for (StyleSpan span : styles) editable.removeSpan(span);
            Matcher matcher = TOKEN.matcher(editable);
            while (matcher.find()) {
                String token = matcher.group(); int color;
                if (token.startsWith("//") || token.startsWith("/*") || token.startsWith("#")) color = 0xFF7F9D82;
                else if (token.startsWith("\"") || token.startsWith("'")) color = 0xFFDBA56B;
                else if (Character.isDigit(token.charAt(0))) color = 0xFFB99AE8;
                else color = 0xFF7FA7E8;
                editable.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), 0);
                if (Character.isLetter(token.charAt(0)) && !token.startsWith("//") && !token.startsWith("/*") && !token.startsWith("#"))
                    editable.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), 0);
            }
        } finally { applying = false; }
    }
}

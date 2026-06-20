package com.sportytomato.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SensitiveWordService {
    private static final String PREFS_NAME = "sensitive_word_prefs";
    private static final List<String> DEFAULT_SENSITIVE_WORDS = Arrays.asList("法轮功", "台独", "藏独", "疆独", "色情", "裸体", "成人", "毒品", "枪支", "弹药");
    private final Set<String> sensitiveWords;
    private final SharedPreferences prefs;

    public SensitiveWordService(Context context) { this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); this.sensitiveWords = new HashSet<>(); loadWordLibrary(); }
    private void loadWordLibrary() { sensitiveWords.addAll(DEFAULT_SENSITIVE_WORDS); }
    public boolean containsSensitiveWord(String text) { if (text == null || text.isEmpty()) return false; String normalized = normalize(text); for (String word : sensitiveWords) { if (word != null && !word.isEmpty() && normalized.contains(word)) return true; } return false; }
    public String normalize(String text) { if (text == null) return ""; String result = text.replaceAll("\\s+", ""); StringBuilder sb = new StringBuilder(); for (char c : result.toCharArray()) { if (c >= 0xFF01 && c <= 0xFF5E) sb.append((char) (c - 0xFEE0)); else if (c == 0x3000) sb.append(' '); else sb.append(c); } return sb.toString().replaceAll("\\s+", "").toLowerCase(); }
    public String validateNickname(String nickname) { if (nickname == null || nickname.trim().isEmpty()) return "昵称不能为空"; if (nickname.trim().length() > 20) return "昵称最多20个字符"; if (containsSensitiveWord(nickname)) return "昵称包含不允许的词语"; return null; }
    public String validateMessage(String message) { if (message == null || message.trim().isEmpty()) return "留言不能为空"; if (message.length() > 100) return "留言最多100个字符"; if (containsSensitiveWord(message)) return "留言包含不允许的词语"; return null; }
}
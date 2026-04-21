package com.azheng.boot.rag.core.prompt;

import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.regex.Pattern;

public class PromptTemplateUtils {
    /**
     * 匹配【连续出现 3 个及以上的换行符】
     * 用于清除多余的空行
     */
    private static final Pattern MULTI_BLANK_LINES = Pattern.compile("(\\n){3,}");


    // 填充占位符为替换值
    public static String fillSlots(String template, Map<String, String> slots) {
        if (template.isEmpty() || template == null) {
            return "";
        }
        if(slots.isEmpty() || slots == null) {
            return template;
        }
        //复制一份新模版
        String result = template;
        // 遍历Map，将模版中"{key}"，替换为value
        for (Map.Entry<String, String> entry : slots.entrySet()) {
            String value = StrUtil.emptyIfNull(entry.getValue());
            result = template.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    public static String cleanupPrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        return MULTI_BLANK_LINES.matcher(prompt).replaceAll("\n\n").trim();
    }
}

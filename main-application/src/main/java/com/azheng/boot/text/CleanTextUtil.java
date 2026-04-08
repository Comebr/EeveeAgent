package com.azheng.boot.text;

import java.nio.charset.StandardCharsets;

/**
 * 文本清洗工具类
 * 可扩展深度清理
 */
public class CleanTextUtil {
    private CleanTextUtil() {
    }

    /**
     * 清理文本内容
     * <p>
     * 执行以下清理操作：
     * 1. 移除 BOM 标记（\uFEFF）
     * 2. 移除行尾多余的空格和制表符
     * 3. 压缩连续的空行（3个以上压缩为2个）
     * 4. 去除首尾空白
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    public static String cleanup(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // 1. 统一编码
        text = new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);


        return text
                // 移除 BOM 标记
                .replace("\uFEFF", "")
                // 移除行尾的空格和制表符
                .replaceAll("[ \\t]+\\n", "\n")
                // 压缩连续的空行（3个以上压缩为2个）
                .replaceAll("\\n{3,}", "\n\n")
                // 去除首尾空白
                .trim();
    }

    /**
     * 清理文本内容（自定义规则）
     *
     * @param text                原始文本
     * @param removeBOM           是否移除 BOM
     * @param trimTrailingSpaces  是否移除行尾空格
     * @param compressEmptyLines  是否压缩空行
     * @param maxConsecutiveLines 最多保留的连续空行数
     * @return 清理后的文本
     */
    public static String cleanup(String text,
                                 boolean removeBOM,
                                 boolean trimTrailingSpaces,
                                 boolean compressEmptyLines,
                                 int maxConsecutiveLines) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;

        if (removeBOM) {
            result = result.replace("\uFEFF", "");
        }

        if (trimTrailingSpaces) {
            result = result.replaceAll("[ \\t]+\\n", "\n");
        }

        if (compressEmptyLines && maxConsecutiveLines > 0) {
            String pattern = "\\n{" + (maxConsecutiveLines + 1) + ",}";
            String replacement = "\n".repeat(maxConsecutiveLines);
            result = result.replaceAll(pattern, replacement);
        }

        return result.trim();
    }
}

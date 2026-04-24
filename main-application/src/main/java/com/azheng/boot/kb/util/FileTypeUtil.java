package com.azheng.boot.kb.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class FileTypeUtil {

    // 定义 标准MIME类型 → 简短文件后缀/名称 的映射（办公文件全覆盖）
    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();

    static {
        // 1. TXT 文本
        MIME_TYPE_MAP.put("text/plain", "txt");

        // 2. PDF 文档
        MIME_TYPE_MAP.put("application/pdf", "pdf");

        // 3. Markdown 文件（兼容两种标准MIME）
        MIME_TYPE_MAP.put("text/markdown", "md");
        MIME_TYPE_MAP.put("text/x-markdown", "md");

        // 4. Word 文档
        MIME_TYPE_MAP.put("application/msword", "doc"); // 旧版doc
        MIME_TYPE_MAP.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"); // 新版docx

        // 5. Excel 表格
        MIME_TYPE_MAP.put("application/vnd.ms-excel", "xls"); // 旧版xls
        MIME_TYPE_MAP.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"); // 新版xlsx
    }

    /**
     * 将长MIME类型转换为简短文件后缀
     * @param mime 原始长MIME类型
     * @return 简短后缀（docx/xlsx等），未知则返回original
     */
    public static String getShortFileType(String mime) {
        return MIME_TYPE_MAP.getOrDefault(mime, mime);
    }
}

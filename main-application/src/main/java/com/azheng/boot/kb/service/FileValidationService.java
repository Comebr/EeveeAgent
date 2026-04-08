package com.azheng.boot.kb.service;

import com.azheng.framework.exception.ClientException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件合法性校验工具类
 */
public class FileValidationService {
    // 允许的文件类型
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            // PDF
            "application/pdf",
            // Word 97-2003
            "application/msword",
            // Word 2007+
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            // Excel 97-2003
            "application/vnd.ms-excel",
            // Excel 2007+
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            // 纯文本
            "text/plain",
            // Markdown 双兼容（标准+旧版）
            "text/markdown",
            "text/x-markdown"
    ));

    // 允许的文件扩展名
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".pdf", ".doc", ".docx", ".md", ".txt", ".xls", ".xlsx"
    ));

    // 最大文件大小 (50MB)
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    // 校验文件
    public static void validateFile(MultipartFile file) throws Exception {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new ClientException("文件为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ClientException("文件大小超过限制，最大允许50MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            // 检查文件扩展名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new ClientException("无法确定文件类型");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new ClientException("文件格式不支持");
            }
        }

        // 检查文件是否损坏
        if (!isFileValid(file)) {
            throw new ClientException("文件损坏或无效");
        }


        // 检查是否为加密文件
        if (isEncrypted(file)) {
            throw new ClientException("不支持加密文件");
        }
    }

    // 检查文件是否有效
    private static boolean isFileValid(MultipartFile file) {
        // 这里可以实现文件完整性检查
        // 例如PDF文件的头部检查、Office文件的格式检查等
        try {
            // 简单检查：尝试读取文件内容
            byte[] bytes = file.getBytes();
            return bytes.length > 0;
        } catch (Exception e) {
            return false;
        }
    }


    // 检查是否为加密文件
    private static boolean isEncrypted(MultipartFile file) {
        // 检查文件是否为加密文件
        // 例如检查PDF文件的加密标志、Office文件的加密状态等
        try {
            // 简单实现，实际项目中需要更详细的检查
            String filename = file.getOriginalFilename();
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                // 检查PDF文件头
                byte[] header = new byte[1024];
                file.getInputStream().read(header);
                // PDF文件加密检查逻辑
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}

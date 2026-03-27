package com.azheng.boot.user.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileUploadUtils {
    public static String upload(MultipartFile file, String uploadPath) throws IOException {
        // 确保上传目录存在
        File directory = new File(uploadPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID() + suffix;

        // 保存文件
        File dest = new File(uploadPath + filename);
        file.transferTo(dest);

        return filename;
    }
}

package com.azheng.framework.user.controller;

import com.azheng.framework.Result;
import com.azheng.framework.user.util.FileUploadUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.allowed-types}")
    private String allowedTypes;

    @Value("${file.upload.max-size}")
    private long maxSize;

    @PostMapping("/avatar/upload")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 检查文件类型
            String contentType = file.getContentType();
            if (!Arrays.asList(allowedTypes.split(",")).contains(contentType)) {
                return Result.error("不支持的文件类型");
            }

            // 检查文件大小
            if (file.getSize() > maxSize) {
                return Result.error("文件大小超过限制");
            }

            // 上传文件
            String filename = FileUploadUtils.upload(file, uploadPath);

            // 返回文件路径
            return Result.success("/uploads/avatar/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败");
        }
    }
}

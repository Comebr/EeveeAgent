package com.azheng.boot.user.controller;

import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import com.azheng.boot.user.util.FileUploadUtils;
import com.azheng.framework.exception.ClientException;
import com.azheng.framework.errorcode.BaseErrorCode;
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
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        // 检查文件类型
        String contentType = file.getContentType();
        if (!Arrays.asList(allowedTypes.split(",")).contains(contentType)) {
            throw new ClientException( "不支持的文件类型");
        }

        // 检查文件大小
        if (file.getSize() > maxSize) {
            throw new ClientException("文件大小超过限制");
        }

        // 上传文件
        String filename = FileUploadUtils.upload(file, uploadPath);

        // 返回文件路径
        return Results.success("/uploads/avatar/" + filename);
    }
}

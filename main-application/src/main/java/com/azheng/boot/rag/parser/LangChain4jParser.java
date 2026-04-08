package com.azheng.boot.rag.parser;

import com.azheng.boot.kb.dao.mapper.FileTORustFSLogMapper;
import com.azheng.framework.exception.ServiceException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

/**
 * lc4j-文档解析
 */
@Component
public class LangChain4jParser {
    public static final String BUCKET_NAME = "a-bucket";

    @Resource
    private FileTORustFSLogMapper fileTORustFSLogMapper;

    @Resource(name = "RustFSClient")
    private S3Client RustFsClient;

    /**
     * Apache Tika 通用解析器
     */
    public Document TikaParser(String rustfsKey){
        //1.从RustFS中加载到本地临时处理
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(BUCKET_NAME).key(rustfsKey).build();

        //使用ApacheTikaDocumentParser
        ApacheTikaDocumentParser apacheTikaDocumentParser = new ApacheTikaDocumentParser();
        try(InputStream inputStream = RustFsClient.getObject(getObjectRequest)){
            return apacheTikaDocumentParser.parse(inputStream);
        }catch (Exception e){
            throw new ServiceException("lc4j文件解析失败");
        }
    }
}

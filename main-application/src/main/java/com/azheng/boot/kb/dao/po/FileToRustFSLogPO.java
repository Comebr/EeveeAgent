package com.azheng.boot.kb.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("file_to_rustfs_log")
public class FileToRustFSLogPO {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Long docId;

    private String rustfsKey;
}

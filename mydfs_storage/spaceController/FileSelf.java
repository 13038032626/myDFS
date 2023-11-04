package com.example.mydfs_storage.spaceController;

import com.example.mydfs_storage.utils.JDBCUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
public class FileSelf {

    @Autowired
    JDBCUtils jdbcUtils;

    Integer totalSize = 1024 * 1024 * 1024;

    Integer startIndex;
    @Value("${storageNum}")
    Integer storageNum;

    List<String> hashes;

    public FileSelf() {
        startIndex = jdbcUtils.getStartIndex();
    }

    public boolean addFile() {
        int end = startIndex + 1024 * 1024 * 6;
        if (end > totalSize) {
            return false;
        }
        startIndex = end;
        return true;
    }


}

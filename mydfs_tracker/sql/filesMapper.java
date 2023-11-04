package com.example.mydfs_tracker.sql;

import com.example.mydfs_tracker.k_vEntitys;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface filesMapper {

    @Select("select * from files")
    List<k_vEntitys> allFiles();

    @Select("select * from files where `key`= ${key}")
    List<k_vEntitys> isExist(String key);

    @Insert("insert into files value (${key},${value},${totalSize},${sliceSize},${uploadedNum}) ")
    boolean insert(k_vEntitys entity);

}

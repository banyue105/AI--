package com.ican.assistant.modules.knowledge;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeTrackMapper {

    @Select("SELECT track_json FROM knowledge_track ORDER BY created_at, id")
    List<String> findAllJson();

    @Select("SELECT track_json FROM knowledge_track WHERE id = #{id}")
    String findJsonById(String id);

    @Insert("""
            INSERT INTO knowledge_track (id, direction_query, track_json)
            VALUES (#{id}, #{query}, #{json})
            ON DUPLICATE KEY UPDATE track_json = #{json}, direction_query = #{query}
            """)
    void save(@Param("id") String id, @Param("query") String query, @Param("json") String json);
}

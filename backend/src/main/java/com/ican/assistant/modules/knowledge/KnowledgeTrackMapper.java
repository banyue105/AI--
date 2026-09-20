package com.ican.assistant.modules.knowledge;

import com.ican.assistant.core.auth.CurrentUser;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeTrackMapper {

    @Select("SELECT track_json FROM knowledge_track WHERE user_id = #{userId} ORDER BY created_at, id")
    List<String> findAllJson(@Param("userId") String userId);

    default List<String> findAllJson() { return findAllJson(CurrentUser.DEMO_USER_ID); }

    @Select("SELECT track_json FROM knowledge_track WHERE user_id = #{userId} AND (id = #{id} OR id = CONCAT(#{userId}, ':', #{id}))")
    String findJsonById(@Param("userId") String userId, @Param("id") String id);

    default String findJsonById(String id) { return findJsonById(CurrentUser.DEMO_USER_ID, id); }

    @Insert("""
            INSERT INTO knowledge_track (user_id, id, direction_query, track_json)
            VALUES (#{userId}, #{id}, #{query}, #{json})
            ON DUPLICATE KEY UPDATE track_json = #{json}, direction_query = #{query}
            """)
    void save(@Param("userId") String userId, @Param("id") String id, @Param("query") String query, @Param("json") String json);
}

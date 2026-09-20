package com.ican.assistant.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProfileMapper {
    record ProfileRow(String id, String name, String avatar, String goalTitle,
                      LocalDate goalDeadline, LocalDateTime updatedAt) {}

    @Select("SELECT * FROM user_profile WHERE id = #{userId}")
    ProfileRow find(@Param("userId") String userId);

    @Select("SELECT id FROM user_profile WHERE id = #{userId} FOR UPDATE")
    String lock(@Param("userId") String userId);

    @Select("SELECT title FROM profile_goal WHERE profile_id = #{userId} ORDER BY position_index")
    List<String> goals(@Param("userId") String userId);

    @Update("UPDATE user_profile SET name=#{name}, avatar=#{avatar}, goal_title=#{goalTitle}, updated_at=CURRENT_TIMESTAMP(6) WHERE id=#{userId}")
    int update(@Param("userId") String userId, @Param("name") String name, @Param("avatar") String avatar, @Param("goalTitle") String goalTitle);

    @Delete("DELETE FROM profile_goal WHERE profile_id = #{userId}")
    void deleteGoals(@Param("userId") String userId);

    @Insert("INSERT INTO profile_goal (profile_id, position_index, title) VALUES (#{userId}, #{position}, #{title})")
    void insertGoal(@Param("userId") String userId, @Param("position") int position, @Param("title") String title);

    @Select("SELECT 1")
    int ping();
}

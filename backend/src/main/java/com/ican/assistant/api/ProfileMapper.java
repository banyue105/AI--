package com.ican.assistant.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProfileMapper {
    record ProfileRow(String id, String name, String avatar, String goalTitle,
                      LocalDate goalDeadline, LocalDateTime updatedAt) {}

    @Select("SELECT * FROM user_profile WHERE id = 'demo-user'")
    ProfileRow find();

    @Select("SELECT id FROM user_profile WHERE id = 'demo-user' FOR UPDATE")
    String lock();

    @Select("SELECT title FROM profile_goal WHERE profile_id = 'demo-user' ORDER BY position_index")
    List<String> goals();

    @Update("UPDATE user_profile SET name=#{name}, avatar=#{avatar}, goal_title=#{goalTitle}, updated_at=CURRENT_TIMESTAMP(6) WHERE id='demo-user'")
    int update(@Param("name") String name, @Param("avatar") String avatar, @Param("goalTitle") String goalTitle);

    @Delete("DELETE FROM profile_goal WHERE profile_id = 'demo-user'")
    void deleteGoals();

    @Insert("INSERT INTO profile_goal (profile_id, position_index, title) VALUES ('demo-user', #{position}, #{title})")
    void insertGoal(@Param("position") int position, @Param("title") String title);

    @Select("SELECT 1")
    int ping();
}

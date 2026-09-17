package com.ican.assistant.modules.abilitygrowth;

import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AbilityMapper {
    record SkillRow(String id, String name, String description, int level, String status,
                    double x, double y, int sortOrder) {}
    record RelationRow(String fromId, String toId, String type, double confidence) {}
    record EvidenceRow(String id, String title, String note, LocalDate createdAt) {}
    record EvidenceLink(String skillId, String evidenceId) {}
    record GoalRow(String title, LocalDate deadline) {}

    @Select("SELECT id, name, description, skill_level AS level, status, x, y, sort_order AS sortOrder FROM ability_skill WHERE user_id = #{userId} ORDER BY sort_order, id")
    List<SkillRow> findSkills(String userId);

    @Select("SELECT from_id AS fromId, to_id AS toId, relation_type AS type, confidence FROM ability_relation WHERE user_id = #{userId} ORDER BY sort_order, from_id, to_id")
    List<RelationRow> findRelations(String userId);

    @Select("SELECT id, title, note, created_at AS createdAt FROM ability_evidence WHERE user_id = #{userId} ORDER BY created_at DESC, id")
    List<EvidenceRow> findEvidence(String userId);

    @Select("SELECT skill_id AS skillId, evidence_id AS evidenceId FROM ability_skill_evidence WHERE user_id = #{userId} ORDER BY skill_id, evidence_id")
    List<EvidenceLink> findEvidenceLinks(String userId);

    @Select("SELECT goal_title AS title, goal_deadline AS deadline FROM user_profile WHERE id = #{userId}")
    GoalRow findGoal(String userId);

    @Select("SELECT updated_at FROM ability_graph_state WHERE user_id = #{userId}")
    LocalDateTime findUpdatedAt(String userId);

    @Select("SELECT user_id FROM ability_graph_state WHERE user_id = #{userId} FOR UPDATE")
    String lockGraph(String userId);

    @Update("UPDATE ability_graph_state SET updated_at = #{updatedAt} WHERE user_id = #{userId}")
    void touch(@Param("userId") String userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Insert("INSERT INTO ability_skill (user_id,id,name,normalized_name,description,skill_level,status,x,y,sort_order) VALUES (#{userId},#{node.id},#{node.name},#{normalizedName},#{node.description},#{node.level},#{node.status},#{node.x},#{node.y},#{sortOrder})")
    void insertSkill(@Param("userId") String userId, @Param("node") AbilityDtos.SkillNode node,
                     @Param("normalizedName") String normalizedName, @Param("sortOrder") int sortOrder);

    @Update("UPDATE ability_skill SET name=#{node.name}, normalized_name=#{normalizedName}, description=#{node.description}, skill_level=#{node.level}, status=#{node.status}, x=#{node.x}, y=#{node.y} WHERE user_id=#{userId} AND id=#{node.id}")
    void updateSkill(@Param("userId") String userId, @Param("node") AbilityDtos.SkillNode node,
                     @Param("normalizedName") String normalizedName);

    @Delete("DELETE FROM ability_skill_evidence WHERE user_id=#{userId} AND skill_id=#{skillId}")
    void deleteSkillEvidence(@Param("userId") String userId, @Param("skillId") String skillId);

    @Insert("INSERT INTO ability_skill_evidence (user_id, skill_id, evidence_id) VALUES (#{userId},#{skillId},#{evidenceId})")
    void insertEvidenceLink(@Param("userId") String userId, @Param("skillId") String skillId,
                            @Param("evidenceId") String evidenceId);

    @Insert("INSERT INTO ability_relation (user_id,from_id,to_id,relation_type,confidence,sort_order) VALUES (#{userId},#{relation.from},#{relation.to},#{relation.type},#{relation.confidence},#{sortOrder})")
    void insertRelation(@Param("userId") String userId, @Param("relation") AbilityDtos.SkillRelation relation,
                        @Param("sortOrder") int sortOrder);

    @Update("UPDATE ability_relation SET confidence=#{relation.confidence} WHERE user_id=#{userId} AND from_id=#{relation.from} AND to_id=#{relation.to} AND relation_type=#{relation.type}")
    void updateRelation(@Param("userId") String userId, @Param("relation") AbilityDtos.SkillRelation relation);

    @Insert("INSERT INTO ability_evidence (user_id,id,title,note,created_at) VALUES (#{userId},#{evidence.id},#{evidence.title},#{evidence.note},#{evidence.createdAt})")
    void insertEvidence(@Param("userId") String userId, @Param("evidence") AbilityDtos.Evidence evidence);
}

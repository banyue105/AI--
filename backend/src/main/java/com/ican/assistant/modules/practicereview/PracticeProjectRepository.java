package com.ican.assistant.modules.practicereview;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PracticeProjectRepository extends JpaRepository<PracticeProjectEntity, String> {
    List<PracticeProjectEntity> findAllByOrderByUpdatedAtDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PracticeProjectEntity p where p.id = :id")
    Optional<PracticeProjectEntity> findLockedById(@Param("id") String id);
}

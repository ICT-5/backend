// src/main/java/ict/project/feedback/core/SimulationQaRepository.java
package ict.project.feedback.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SimulationQaRepository extends JpaRepository<SimulationQuestion, Long> {

    @Query(value = """
        SELECT sq.sim_question_id AS qid,
               sa.content         AS answer
          FROM simulation_question sq
          JOIN simulation_answer sa
            ON sa.sim_question_id = sq.sim_question_id
         WHERE sq.session_id = :sessionId
           AND sa.content IS NOT NULL
         ORDER BY sq.sim_question_id ASC
        """, nativeQuery = true)
    List<QaProjection> findAllBySessionId(@Param("sessionId") Long sessionId);
}

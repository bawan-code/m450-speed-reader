package bbzbl_it_m450.speedreader.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bbzbl_it_m450.speedreader.model.ReadingSession;

public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {

    List<ReadingSession> findByUserIdOrderByDateDesc(Long userId);

    List<ReadingSession> findTop5ByUserIdOrderByDateDesc(Long userId);

    List<ReadingSession> findByUserId(Long userId);
}

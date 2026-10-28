package bbzbl_it_m450.speedreader.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import bbzbl_it_m450.speedreader.model.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndTextId(Long userId, Long textId);
}

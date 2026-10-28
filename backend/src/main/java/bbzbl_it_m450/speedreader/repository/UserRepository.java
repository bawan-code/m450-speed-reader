package bbzbl_it_m450.speedreader.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import bbzbl_it_m450.speedreader.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}

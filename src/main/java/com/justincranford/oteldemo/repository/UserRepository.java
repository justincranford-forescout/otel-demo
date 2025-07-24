package com.justincranford.oteldemo.repository;

import com.justincranford.oteldemo.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@SuppressWarnings({"unused"})
public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByName(String caseSensitiveName);
    List<User> findByNameContaining(String caseSensitiveNameSubstring);

    @Query("SELECT u FROM User u WHERE LOWER(u.name) = LOWER(:caseInsensitiveName)")
    List<User> findByNameIgnoreCase(String caseInsensitiveName);
    List<User> findByNameIgnoreCaseContaining(String caseInsensitiveNameSubstring);
}

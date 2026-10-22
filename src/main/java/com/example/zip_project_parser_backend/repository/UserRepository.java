package com.example.zip_project_parser_backend.repository;

import com.example.zip_project_parser_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubId(Long githubId);

}

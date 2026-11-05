package com.MovieMatic.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MovieMatic.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

	boolean existsByEmail(String email);
}
	
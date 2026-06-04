package com.solofounder.horseracing.repository;


import com.solofounder.horseracing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
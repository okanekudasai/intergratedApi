package com.okane.intergratedapi.testing;

import com.okane.intergratedapi.testing.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}

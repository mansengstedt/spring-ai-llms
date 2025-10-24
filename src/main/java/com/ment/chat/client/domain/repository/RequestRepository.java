package com.ment.chat.client.domain.repository;

import com.ment.chat.client.domain.Request;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<Request, String> {
}

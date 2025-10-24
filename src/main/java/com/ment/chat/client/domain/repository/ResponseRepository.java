package com.ment.chat.client.domain.repository;

import com.ment.chat.client.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseRepository extends JpaRepository<Response, String> {
}

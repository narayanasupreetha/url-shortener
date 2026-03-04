package com.urlshortener.repository;

import com.urlshortener.entity.UrlClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlClickEventRepository extends JpaRepository<UrlClickEvent, Long> {
}

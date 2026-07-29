package com.aiprototype.urlshortener.repository;

import com.aiprototype.urlshortener.domain.ClickEvent;
import com.aiprototype.urlshortener.domain.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    List<ClickEvent> findByShortUrlOrderByClickedAtDesc(ShortUrl shortUrl);

    Optional<ClickEvent> findTopByShortUrlOrderByClickedAtDesc(ShortUrl shortUrl);
}

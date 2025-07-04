package com.melishorturlapi.repository;

import com.melishorturlapi.model.ShortUrl;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@EnableScan
public interface ShortUrlRepository extends CrudRepository<ShortUrl, String> {

    // find short URL by short code
    ShortUrl findByShortUrl(String shortUrl);

    // delete short URL by short code
    void deleteByShortUrl(String shortUrl);

    ShortUrl findByOriginalUrl(String originalUrl);
}
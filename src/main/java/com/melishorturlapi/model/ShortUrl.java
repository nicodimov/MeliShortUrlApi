package com.melishorturlapi.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBTable(tableName = "ShortUrls")
public class ShortUrl {

    private String shortUrl;
    private String originalUrl;
    private Long createdAt;
    private Long redirectCount;

    @DynamoDBHashKey(attributeName = "ShortUrl")
    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    @DynamoDBAttribute(attributeName = "OriginalUrl")
    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    @DynamoDBAttribute(attributeName = "CreatedAt")
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDBAttribute(attributeName = "RedirectCount")
    public Long getRedirectCount() {
        return redirectCount;
    }

    public void setRedirectCount(Long redirectCount) {
        this.redirectCount = redirectCount;
    }
}
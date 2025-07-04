package com.melishorturlapi.config;

import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import javax.annotation.PostConstruct;
import com.amazonaws.auth.BasicAWSCredentials;

@Configuration
@EnableDynamoDBRepositories(basePackages = "com.melishorturlapi.repository")
public class DynamoDBConfig {

    @Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;

    @Value("${amazon.aws.region}")
    private String amazonAWSRegion;

    @Value("${amazon.aws.accesskey}")
    private String accesKey;

    @Value("${amazon.aws.secretkey}")
    private String secretkey;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accesKey, secretkey);
        return AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(amazonDynamoDBEndpoint, amazonAWSRegion))
            // .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
            .build();
    }

    @Bean
    @Primary
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
        return new DynamoDBMapper(amazonDynamoDB);
    }

    @Configuration
    public class DynamoDBTableInitializer {
        private final AmazonDynamoDB amazonDynamoDB;

        public DynamoDBTableInitializer(AmazonDynamoDB amazonDynamoDB) {
            this.amazonDynamoDB = amazonDynamoDB;
        }

        @PostConstruct
        public void createTableIfNotExists() {
            String tableName = "ShortUrls";
            try {
                amazonDynamoDB.describeTable(tableName);
                // Table exists
            } catch (ResourceNotFoundException e) {
                // Table does not exist, create it
                CreateTableRequest request = new CreateTableRequest()
                        .withTableName(tableName)
                        .withKeySchema(new KeySchemaElement("ShortUrl", KeyType.HASH))
                        .withAttributeDefinitions(new AttributeDefinition("ShortUrl", ScalarAttributeType.S))
                        .withBillingMode(BillingMode.PAY_PER_REQUEST);
                amazonDynamoDB.createTable(request);
            }
        }
    }
}
package com.sample.productsmanagement.service.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

  @Value("${aws.access.key}")
  private String awsAccessKey;

  @Value("${aws.secret.key}")
  private String awsSecretKey;

  @Value("${aws.s3.region}")
  private String awsRegion;

  @Value("${aws.s3.endpoint}")
  private String endpoint;

  @Bean(name = "s3Client")
  public AmazonS3 s3Client() {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
    EndpointConfiguration endpointConfiguration  = new EndpointConfiguration(endpoint, awsRegion);

    AmazonS3 awsS3Config = AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withEndpointConfiguration(endpointConfiguration)
        .build();

    return awsS3Config;
  }
}
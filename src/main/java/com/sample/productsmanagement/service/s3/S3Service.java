package com.sample.productsmanagement.service.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class S3Service {

  @Autowired
  private AmazonS3 s3Client;

  @Value("${aws.s3.bucket}")
  private String bucketName;

  public String uploadFile(String filename, MultipartFile file) throws IOException {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getBytes());
    s3Client.putObject(bucketName, filename, inputStream, null);
    return this.getPreSignedUrl(filename);
  }

  public String getPreSignedUrl(String filename) {
    if (Strings.isEmpty(filename)) {
      return null;
    }

    Date expirationDate = Date.from(Instant.now().plus(Duration.ofMinutes(10)));
    return s3Client.generatePresignedUrl(bucketName, filename, expirationDate, HttpMethod.GET)
        .toString();
  }

  public void deleteFile(String fileName) {
    if (Strings.isNotEmpty(fileName)) {
      s3Client.deleteObject(bucketName, fileName);
    }
  }
}

package com.sample.productsmanagement.s3;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.sample.productsmanagement.service.s3.S3Service;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
class S3ServiceTest {
  @InjectMocks
  private S3Service s3Service;

  @Mock
  private AmazonS3 s3Client;

  @BeforeEach
  public void setup(){
    ReflectionTestUtils.setField(s3Service, "bucketName", "testBucket");
  }

  @Test
  @DisplayName("Upload file expect return pre signed url when success upload file to s3")
  public void uploadFile_expectReturnUrl() throws Exception {
    URL url = new URI("https://test.example.com").toURL();
    MockMultipartFile mockMultipartFile = new MockMultipartFile("example", "".getBytes());
    when(s3Client.generatePresignedUrl(eq("testBucket"), eq("test"), any(), eq(HttpMethod.GET))).thenReturn(url);

    Assertions.assertEquals("https://test.example.com", s3Service.uploadFile("test", mockMultipartFile));
  }

  @Test
  @DisplayName("Get PreSigned Url expect return null when try to create pre signed null file")
  public void getPreSignedUrl_expectReturnNull(){
    Assertions.assertNull(s3Service.getPreSignedUrl(null));
  }

  @Test
  @DisplayName("Delete File expect call s3 delete object method when called")
  public void deleteFile_expectCallS3DeleteObject(){
    s3Service.deleteFile("test");

    verify(s3Client, times(1)).deleteObject("testBucket", "test");
  }
}
package com.sample.productsmanagement.integrationTest;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sample.productsmanagement.model.Product;
import com.sample.productsmanagement.model.ProductDTO;
import com.sample.productsmanagement.repository.ProductRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.bytebuddy.utility.dispatcher.JavaDispatcher.Container;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductControllerIT {

  @LocalServerPort
  private Integer port;

  private String BASE_URL;

  static final String BUCKET_NAME = UUID.randomUUID().toString();

  private RestTemplate restTemplate;

  static MySQLContainer mySQLContainer = new MySQLContainer("mysql:latest");
  static LocalStackContainer localStackContainer = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:3.0")
  );

  @BeforeAll
  static void beforeAll() throws IOException, InterruptedException {
    mySQLContainer.start();
    localStackContainer.start();
    localStackContainer.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
  }

  @AfterAll
  static void afterAll() {
    mySQLContainer.stop();
    localStackContainer.stop();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", mySQLContainer::getUsername);
    registry.add("spring.datasource.password", mySQLContainer::getPassword);
    registry.add("aws.access.key", localStackContainer::getAccessKey);
    registry.add("aws.secret.key", localStackContainer::getSecretKey);
    registry.add("aws.s3.endpoint", localStackContainer::getEndpoint);
    registry.add("aws.s3.region", localStackContainer::getRegion);
    registry.add("aws.s3.bucket", () -> BUCKET_NAME);
  }

  @Autowired
  ProductRepository productRepository;

  @Autowired
  AmazonS3 s3Client;

  @BeforeEach
  void setUp() {
    BASE_URL = String.format("http://localhost:%s/api/v1/products", port);
    restTemplate = new RestTemplate();
    productRepository.deleteAll();
    List<S3ObjectSummary> objectSummaries = s3Client.listObjects(BUCKET_NAME).getObjectSummaries();
    objectSummaries.forEach(object -> s3Client.deleteObject(BUCKET_NAME, object.getKey()));
  }

  @Value("classpath:/sample-image/sample.jpeg")
  private Resource testImage;

  private HttpEntity createRequestBody(ProductDTO product, Resource productImage) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    final MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
    requestBody.set("file", productImage);
    requestBody.set("name", product.getName());
    requestBody.set("price", String.valueOf(product.getPrice()));
    requestBody.set("quantity", String.valueOf(product.getQuantity()));

    return new HttpEntity<>(requestBody, headers);
  }

  private ByteArrayResource createEmptyResource() {
    return new ByteArrayResource(new byte[0]) {
      @Override
      public String getFilename() {
        return "empty-image.png";
      }
    };
  }

  @Test
  @DisplayName("Create new Product Expect store record in db and file when success")
  public void addNewProduct_expectRecordStoredOnDbAndImageOnS3() {
    ProductDTO newProduct = ProductDTO.builder()
        .name("tests")
        .price(1)
        .quantity(1)
        .build();
    HttpEntity requestBody = createRequestBody(newProduct, testImage);
    ResponseEntity<ProductDTO> response = restTemplate.postForEntity(BASE_URL, requestBody,
        ProductDTO.class);
    String fileName = String.format("%s-%s", response.getBody().getId(), testImage.getFilename());

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertNotNull(s3Client.getObject(BUCKET_NAME, fileName));
    Assertions.assertNotNull(productRepository.findById(response.getBody().getId()).isPresent());
  }

  @Test
  @DisplayName("Update product expect update record on db and delete file on S3 when image removed")
  public void updateProduct_expectImageEmptyWhenRemovingImageFile() {
    ProductDTO newProductRequest = ProductDTO.builder()
        .name("tests")
        .price(1)
        .quantity(1)
        .build();
    HttpEntity requestBody = createRequestBody(newProductRequest, testImage);
    ResponseEntity<ProductDTO> response = restTemplate.postForEntity(BASE_URL, requestBody,
        ProductDTO.class);
    ProductDTO productDTOResponse = response.getBody();

    newProductRequest.setName("test-test");
    HttpEntity updateRequest = createRequestBody(newProductRequest, createEmptyResource());
    restTemplate.put(String.format("%s/%s", BASE_URL, productDTOResponse.getId()), updateRequest);
    Product savedProduct = productRepository.findById(productDTOResponse.getId()).get();

    Assertions.assertEquals("test-test", savedProduct.getName());
    Assertions.assertNull(savedProduct.getImageLocation());
    Assertions.assertEquals(0, s3Client.listObjects(BUCKET_NAME).getObjectSummaries().size());
  }

  @Test
  @DisplayName("Update product expect update record on db and update file on S3 when image replaced")
  public void updateProduct_expectImageReplaced() {
    ProductDTO newProductRequest = ProductDTO.builder()
        .name("tests")
        .price(1)
        .quantity(1)
        .build();
    HttpEntity requestBody = createRequestBody(newProductRequest, testImage);
    ResponseEntity<ProductDTO> response = restTemplate.postForEntity(BASE_URL, requestBody,
        ProductDTO.class);
    ProductDTO productDTOResponse = response.getBody();
    Resource newImage = new ClassPathResource("sample-image/sample-2.jpeg");

    HttpEntity updateRequest = createRequestBody(newProductRequest, newImage);
    restTemplate.put(String.format("%s/%s", BASE_URL, productDTOResponse.getId()), updateRequest);
    Product savedProduct = productRepository.findById(productDTOResponse.getId()).get();
    String expectedImageName = String.format("%s-%s", savedProduct.getId(), "sample-2.jpeg");

    Assertions.assertEquals(expectedImageName, savedProduct.getImageLocation());
    Assertions.assertEquals(1, s3Client.listObjects(BUCKET_NAME).getObjectSummaries().size());
    Assertions.assertNotNull(s3Client.getObject(BUCKET_NAME, savedProduct.getImageLocation()));
  }

  @Test
  @DisplayName("Get All product expected to get get all product stored on database")
  public void GetAllProducts_expectGetListOfProducts() {
    List<Product> productsList = List.of(
        Product.builder().name("test 1").price(1).quantity(1).build(),
        Product.builder().name("test 2").price(1).quantity(1).build()
    );
    productRepository.saveAll(productsList);

    ResponseEntity<List> response = restTemplate.getForEntity(BASE_URL, List.class);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals(2, response.getBody().size());
  }

  @Test
  @DisplayName("Get All product expected to get same same product on db when given product db exists")
  public void GetProductsById_expectGetProductDetails() {
    Product product = Product.builder().name("test 1").price(1).quantity(1).build();
    ProductDTO savedProduct = productRepository.save(product).convertToDTO();
    String fullUrl = String.format("%s/%s", BASE_URL, savedProduct.getId());
    ResponseEntity<ProductDTO> response = restTemplate.getForEntity(fullUrl, ProductDTO.class);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals(savedProduct, response.getBody());
  }

  @Test
  @DisplayName("Delete product should be delete record on db and remove file on S3 when delete success")
  public void deleteProductById_expectDeleteDBRecordAndS3FileForRelatedId() throws IOException {
    String fileName = String.format("%s-%s", "1", "test.png");
    s3Client.putObject(BUCKET_NAME, fileName, testImage.getFile());
    Product product = Product.builder()
        .name("test 1")
        .price(1)
        .quantity(1)
        .imageLocation(fileName)
        .build();
    Product savedProduct = productRepository.save(product);

    restTemplate.delete(BASE_URL + "/1");

    Assertions.assertTrue(productRepository.findById(savedProduct.getId()).isEmpty());
    AmazonS3Exception exception = Assertions.assertThrows(AmazonS3Exception.class,
        () -> s3Client.getObject(BUCKET_NAME, savedProduct.getImageLocation()));
    Assertions.assertEquals(404, exception.getStatusCode());
    Assertions.assertEquals("NoSuchKey", exception.getErrorCode());
  }
}

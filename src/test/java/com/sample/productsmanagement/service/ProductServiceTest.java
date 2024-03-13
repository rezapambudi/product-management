package com.sample.productsmanagement.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sample.productsmanagement.exception.ProductNotFoundException;
import com.sample.productsmanagement.model.Product;
import com.sample.productsmanagement.repository.ProductRepository;
import com.sample.productsmanagement.service.product.ProductService;
import com.sample.productsmanagement.service.s3.S3Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @InjectMocks
  private ProductService productService;

  @Mock
  private S3Service s3Service;

  @Mock
  private ProductRepository productRepository;

  @Test
  @DisplayName("Create Product expect save product and send response with product image url")
  void createProduct_expectSaveProductAndSendProductWithImageUrl() throws IOException {
    Product newProduct = Product.builder()
        .name("test")
        .price(1)
        .quantity(10)
        .build();
    MockMultipartFile newProductImage = new MockMultipartFile("new image", "product.png", "image/png", "test".getBytes());
    when(productRepository.save(any(Product.class))).thenReturn(
        new Product(1, "test", 1, 10, null),
        new Product(1, "test", 1, 10, "1-product.png")
    );
    when(s3Service.uploadFile("1-product.png", newProductImage)).thenReturn("s3.aws.com/1-product.png");
    Product expectedProduct = new Product(1, "test", 1, 10, "s3.aws.com/1-product.png");

    Assertions.assertEquals(expectedProduct, productService.createProduct(newProduct, newProductImage));
  }

  @Test
  @DisplayName("Get Product expect return product detail when given product id exists")
  public void getProductDetail_expectReturnProductDetail() {
    Product product = new Product(1, "test", 1, 1, "1-image.png");
    when(productRepository.findById(1)).thenReturn(Optional.of(product));
    when(s3Service.getPreSignedUrl("1-image.png")).thenReturn("s3.aws.com/1-image.png");
    Product expectedProduct = new Product(1, "test", 1, 1, "s3.aws.com/1-image.png");

    Assertions.assertEquals(expectedProduct, productService.getProductDetail(1));
  }

  @Test
  @DisplayName("Get Product expect got product not found exception when product given id not exist")
  void getProductDetail_expectThrowProductNotFoundException() {
    ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
        () -> productService.getProductDetail(1));
    Assertions.assertEquals("Product with id 1, not found", exception.getMessage());
  }

  @Test
  @DisplayName("Update Product expect return product with image url when update product without url")
  void updateProduct_expectUpdateProductImageURL() throws IOException {
    Product existingProduct = new Product(2, "test 2", 1, 1, "1-image.png");
    Product updatedProduct = new Product(2, "update test", 1, 2, null);
    MockMultipartFile mockMultipartFile = new MockMultipartFile("test.png", "test.png",
        "images/png", "afds".getBytes());
    when(productRepository.findById(2)).thenReturn(Optional.of(existingProduct));
    when(s3Service.uploadFile("2-test.png", mockMultipartFile)).thenReturn(
        "s3.aws.com/2-test.png");
    Product expectedResult = new Product(2, "update test", 1, 2, "s3.aws.com/2-test.png");

    Assertions.assertEquals(expectedResult,
        productService.updateProduct(updatedProduct, mockMultipartFile));
    verify(s3Service, times(1)).deleteFile("1-image.png");
    verify(productRepository, times(1)).findById(2);
  }

  @Test
  @DisplayName("Update Product expect return product with no image url when update product with image url")
  void updateProduct_expectSetProductImageUrlToNull() throws IOException {
    Product existingProduct = new Product(1, "test", 1, 1, "1-image.png");
    Product updatedProduct = new Product(1, "update test", 1, 2, null);
    when(productRepository.findById(1)).thenReturn(Optional.of(existingProduct));
    MockMultipartFile mockMultipartFile = new MockMultipartFile("test", "".getBytes());

    Assertions.assertEquals(updatedProduct,
        productService.updateProduct(updatedProduct, mockMultipartFile));
    verify(productRepository, times(1)).findById(1);
    verify(s3Service, times(1)).deleteFile("1-image.png");
    verify(productRepository, times(1)).save(updatedProduct);
  }

  @Test
  @DisplayName("Update Product expect got product not found exception when update product given id not exist")
  void updateProduct_expectThrowProductNotFoundException() {
    ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
        () -> productService.updateProduct(Product.builder().id(1).build(),
            new MockMultipartFile("test", "".getBytes())));
    Assertions.assertEquals("Product with id 1, not found", exception.getMessage());
  }

  @Test
  @DisplayName("Delete Product expect call s3 service and product repository delete when success delete product given id")
  void deleteProduct_expectCallS3ServiceDeleteMethod() {
    Product product = new Product(1, "test", 1, 1, "1-image.png");
    when(productRepository.findById(1)).thenReturn(Optional.of(product));
    productService.deleteProduct(1);

    verify(productRepository, times(1)).deleteById(1);
    verify(s3Service, times(1)).deleteFile("1-image.png");
  }

  @Test
  @DisplayName("Delete Product expect got product not found exception when delete product given id not exist")
  void deleteProduct_expectThrowProductNotFoundException() {
    ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
        () -> productService.deleteProduct(1));
    Assertions.assertEquals("Product with id 1, not found", exception.getMessage());
  }

  @Test
  @DisplayName("Get products expect return all list of products when called")
  void getProducts_expectReturnAllProductList() {
    Product product1 = new Product(1, "test 1", 1, 1, "1-example.png");
    Product product2 = new Product(2, "test 2", 1, 1, "2-example.png");
    Product expectedProduct1 = new Product(1, "test 1", 1, 1, "s3.aws.com/1-example.png");
    Product expectedProduct2 = new Product(2, "test 2", 1, 1, "s3.aws.com/2-example.png");
    List<Product> productList = Arrays.asList(product1, product2);
    List<Product> expected = Arrays.asList(expectedProduct1, expectedProduct2);
    when(productRepository.findAll()).thenReturn(productList);
    when(s3Service.getPreSignedUrl(anyString())).thenReturn(
        "s3.aws.com/1-example.png",
        "s3.aws.com/2-example.png"
    );

    productService.getProducts()
        .forEach(product -> Assertions.assertTrue(expected.contains(product)));
  }
}
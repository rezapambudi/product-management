package com.sample.productsmanagement.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sample.productsmanagement.exception.ProductNotFoundException;
import com.sample.productsmanagement.model.Product;
import com.sample.productsmanagement.service.product.ProductService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  private ProductService productService;

  @Value("classpath:/product-fixture/product-response-list.json")
  private Resource responseSample;

  private MockMultipartHttpServletRequestBuilder createPutRequestWithMultipart(int productId) {
    String updateUrl = String.format("/api/v1/products/%s", productId);
    MockMultipartHttpServletRequestBuilder putRequest = MockMvcRequestBuilders.multipart(updateUrl);
    putRequest.with(request -> {
      request.setMethod("PUT");
      return request;
    });

    return putRequest;
  }

  @Test
  @DisplayName("Get all product expect list of products")
  void getProducts_ExpectReturnListProduct() throws Exception {
    Product product1 = new Product(1, "test product 1", 1, 0, "https://example.com/1-images.png");
    Product product2 = new Product(2, "test product 2", 1, 0, "https://example.com/2-images.png");
    when(productService.getProducts()).thenReturn(Arrays.asList(product1, product2));
    String sampleResponse = responseSample.getContentAsString(StandardCharsets.UTF_8);

    mockMvc.perform(get("/api/v1/products"))
        .andExpect(status().isOk())
        .andExpect(content().json(sampleResponse, true));
  }

  @Test
  @DisplayName("Get all product expect return empty array when there is no product list")
  void getProducts_ExpectReturnEmptyArray() throws Exception {
    when(productService.getProducts()).thenReturn(new ArrayList<>());

    mockMvc.perform(get("/api/v1/products"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]", true));
  }

  @Test
  @DisplayName("Get product by id return product detail when product given id exists")
  void getProductById_ExpectReturnProductDetail() throws Exception {
    String sampleResponse = responseSample.getContentAsString(StandardCharsets.UTF_8);
    JSONObject jsonObject = (JSONObject) new JSONArray(sampleResponse).get(0);
    Product product = new Product(1, "test product 1", 1, 0, "https://example.com/1-images.png");
    when(productService.getProductDetail(eq(1))).thenReturn(product);

    mockMvc.perform(get("/api/v1/products/1"))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonObject.toString(), true));
  }

  @Test
  @DisplayName("Get product by id got response product not found when trying get non exist product Id")
  void getProductDetails_ExpectGotNotFound() throws Exception {
    String expectedResponseMessage = "{\"message\":\"Product with id 99, not found\"}";
    when(productService.getProductDetail(eq(99))).thenThrow(
        new ProductNotFoundException("Product with id 99, not found"));

    mockMvc.perform(get("/api/v1/products/99"))
        .andExpect(status().isNotFound())
        .andExpect(content().json(expectedResponseMessage, true));
  }

  @Test
  @DisplayName("Create new product got success response")
  void createNewProduct_ExpectSuccessCreateNewProduct() throws Exception {
    MockMultipartFile mockMultipart = new MockMultipartFile("file", "".getBytes());
    Product newProduct = Product.builder()
        .name("sample")
        .price(1)
        .quantity(1)
        .build();
    Product storedProduct = new Product(33, "sample", 1, 1, null);
    when(productService.createProduct(newProduct, mockMultipart)).thenReturn(storedProduct);
    String expectedMessage = "{\"id\":33,\"name\":\"sample\",\"price\":1,\"quantity\":1,\"imageUrl\":null}";

    mockMvc.perform(multipart("/api/v1/products")
            .file(mockMultipart)
            .param("name", "sample")
            .param("price", "1")
            .param("quantity", "1")
        ).andExpect(status().isOk())
        .andExpect(content().json(expectedMessage, true));
  }

  @Test
  @DisplayName("Create new product got number format exception when passing non number format to price")
  void createNewProduct_ExpectGotNumberFormatException() throws Exception {
    MockMultipartFile mockMultipart = new MockMultipartFile("file", "".getBytes());
    String expectedMessage = "{\"message\":\"Failed to convert property value of type 'java.lang.String' to required type 'int' for property 'price'; For input string: \\\"a\\\"\"}";

    mockMvc.perform(multipart("/api/v1/products")
            .file(mockMultipart)
            .param("name", "tests")
            .param("price", "a")
            .param("quantity", "1")
        ).andExpect(status().isBadRequest())
        .andExpect(content().json(expectedMessage, true));
  }

  @Test
  @DisplayName("Create new product got response bad request when there is violation on request")
  void createNewProduct_ExpectGotViolationError() throws Exception {
    MockMultipartFile mockMultipart = new MockMultipartFile("file", "".getBytes());
    List<String> violationMessageList = Arrays.asList(
        "Minimum product name length is 5",
        "Minimum product price is 1"
    );

    MvcResult response = mockMvc.perform(multipart("/api/v1/products")
        .file(mockMultipart)
        .param("name", "tes")
        .param("price", "0")
        .param("quantity", "1")
    ).andReturn();
    String responseBody = response.getResponse().getContentAsString();

    Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getResponse().getStatus());
    violationMessageList.forEach(errorMessage -> {
      Assertions.assertTrue(responseBody.contains(errorMessage));
    });
  }

  @Test
  @DisplayName("Update product by id status success and got updated response")
  void updateProductById_ExpectReturnUpdatedProduct() throws Exception {
    MockMultipartFile mockMultipart = new MockMultipartFile("file", "product.png", "image/png",
        "{}".getBytes());
    Product productRequest = new Product(99, "sample", 1, 1, null);
    Product updatedProduct = new Product(99, "sample", 1, 1, "https://example.com/99-product.png");
    when(productService.updateProduct(productRequest, mockMultipart)).thenReturn(updatedProduct);
    String expectedResponse = "{\"id\":99,\"name\":\"sample\",\"price\":1,\"quantity\":1,\"imageUrl\":\"https://example.com/99-product.png\"}";

    mockMvc.perform(createPutRequestWithMultipart(99)
            .file(mockMultipart)
            .param("name", "sample")
            .param("price", "1")
            .param("quantity", "1")
        ).andExpect(status().isOk())
        .andExpect(content().json(expectedResponse, true));
  }

  @Test
  @DisplayName("Update product by id got response bad request when there is violation on request")
  void updateProductById_ExpectGotViolationError() throws Exception {
    MockMultipartFile mockMultipart = new MockMultipartFile("file", "".getBytes());
    List<String> violationMessageList = Arrays.asList(
        "Product quantity should not less than 0",
        "Minimum product price is 1",
        "Minimum product name length is 5");

    MvcResult response = mockMvc.perform(createPutRequestWithMultipart(99)
        .file(mockMultipart)
        .param("name", "tes")
        .param("price", "0")
        .param("quantity", "-1")
    ).andReturn();
    String responseBody = response.getResponse().getContentAsString();

    Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getResponse().getStatus());
    violationMessageList.forEach(errorMessage -> {
      Assertions.assertTrue(responseBody.contains(errorMessage));
    });
  }

  @Test
  @DisplayName("update product by id got response product bad request when trying upload non image file")
  void updateProductById_ExpectGotBadRequest_WhenUploadedNotImageFile() throws Exception {
    String expectedResponseMessage = "{\"message\":\"Product image should be image format but got, application/json\"}";
    MockMultipartFile mockMultipart = new MockMultipartFile("file", "sample.json",
        "application/json", "{}".getBytes());
    Product product = Product.builder()
        .id(99)
        .name("tests")
        .price(1)
        .quantity(1)
        .build();
    when(productService.updateProduct(product, mockMultipart)).thenThrow(
        new ProductNotFoundException("Product with id 99, not found"));
    MockMultipartHttpServletRequestBuilder putMultipart = createPutRequestWithMultipart(99);

    mockMvc.perform(putMultipart
            .file(mockMultipart)
            .param("name", "tests")
            .param("price", "1")
            .param("quantity", "1")
        ).andExpect(status().isBadRequest())
        .andExpect(content().json(expectedResponseMessage, true));
  }

  @Test
  @DisplayName("update product by id got response product not found when trying update not exist product")
  void updateProductById_ExpectGotNotFound() throws Exception {
    String expectedResponseMessage = "{\"message\":\"Product with id 99, not found\"}";
    MockMultipartFile mockMultipart = new MockMultipartFile("file", "".getBytes());
    Product product = Product.builder()
        .id(99)
        .name("tests")
        .price(1)
        .quantity(1)
        .build();
    when(productService.updateProduct(product, mockMultipart)).thenThrow(
        new ProductNotFoundException("Product with id 99, not found"));
    MockMultipartHttpServletRequestBuilder putMultipart = createPutRequestWithMultipart(99);

    mockMvc.perform(putMultipart
            .file(mockMultipart)
            .param("name", "tests")
            .param("price", "1")
            .param("quantity", "1")
        ).andExpect(status().isNotFound())
        .andExpect(content().json(expectedResponseMessage, true));
  }


  @Test
  @DisplayName("Delete product by id got response success when product given id exists")
  void deleteProductById_ExpectGotSuccess() throws Exception {
    String expectedResponseMessage = "{\"message\":\"Delete product with id 99 success\"}";
    doNothing().when(productService).deleteProduct(99);

    mockMvc.perform(delete("/api/v1/products/99"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedResponseMessage, true));
  }

  @Test
  @DisplayName("Delete product by id got response product not found when trying delete not exist product")
  void deleteProductById_ExpectGotNotFound() throws Exception {
    String expectedResponseMessage = "{\"message\":\"Product with id 99, not found\"}";
    doThrow(new ProductNotFoundException("Product with id 99, not found"))
        .when(productService)
        .deleteProduct(99);

    mockMvc.perform(delete("/api/v1/products/99"))
        .andExpect(status().isNotFound())
        .andExpect(content().json(expectedResponseMessage, true));
  }
}
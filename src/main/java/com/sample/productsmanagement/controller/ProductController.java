package com.sample.productsmanagement.controller;

import com.mysql.cj.util.StringUtils;
import com.sample.productsmanagement.exception.InvalidFileFormatException;
import com.sample.productsmanagement.model.Product;
import com.sample.productsmanagement.model.ProductDTO;
import com.sample.productsmanagement.service.product.ProductService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {

  @Autowired
  private ProductService productService;

  private void checkFileFormat(MultipartFile productImage) {
    String fileType = productImage.getContentType() == null? "":productImage.getContentType();
    boolean isImages = StringUtils.startsWithIgnoreCase(fileType, "image");
    if(!productImage.isEmpty() && !isImages){
      throw new InvalidFileFormatException(fileType);
    }
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,  produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ProductDTO> createNewProduct(@Valid @ModelAttribute ProductDTO productDto,
      @RequestParam("file") MultipartFile productImage) throws IOException {
    checkFileFormat(productImage);
    Product productCreationDto = productDto.mapToProduct();
    Product storedProduct = productService.createProduct(productCreationDto, productImage);

    return ResponseEntity.ok(storedProduct.convertToDTO());
  }

  @GetMapping(path = "/{productId}",  produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ProductDTO> getProductDetailsById(@PathVariable("productId") int productId) {
    Product storedProduct = productService.getProductDetail(productId);

    return ResponseEntity.ok(storedProduct.convertToDTO());
  }

  @GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<ProductDTO>> getProducts() {
    List<Product> products = productService.getProducts();
    List<ProductDTO> productDTOList = products.stream().map(Product::convertToDTO).toList();

    return ResponseEntity.ok(productDTOList);
  }

  @PutMapping(path = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,  produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ProductDTO> updateProduct(@PathVariable("productId") int productId,
      @ModelAttribute @Valid ProductDTO productDto, @RequestParam("file") MultipartFile productImage)
      throws IOException {
    checkFileFormat(productImage);
    Product product = productDto.mapToProduct();
    product.setId(productId);
    Product updatedProduct = productService.updateProduct(product, productImage);

    return ResponseEntity.ok(updatedProduct.convertToDTO());
  }

  @DeleteMapping(value = "/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> deleteProduct(@PathVariable("productId") int productId) {
    productService.deleteProduct(productId);
    HashMap<String, String> message = new HashMap<>();
    message.put("message", String.format("Delete product with id %s success", productId));

    return ResponseEntity.ok(message);
  }
}

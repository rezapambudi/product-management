package com.sample.productsmanagement.service.product;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.mysql.cj.util.StringUtils;
import com.sample.productsmanagement.exception.InvalidFileFormatException;
import com.sample.productsmanagement.exception.ProductNotFoundException;
import com.sample.productsmanagement.model.Product;
import com.sample.productsmanagement.repository.ProductRepository;
import com.sample.productsmanagement.service.s3.S3Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private S3Service s3Service;


  private Product getProductById(int id) {
    Optional<Product> storedProduct = productRepository.findById(id);
    if (storedProduct.isEmpty()) {
      String errorMessage = String.format("Product with id %s, not found", id);
      throw new ProductNotFoundException(errorMessage);
    }

    return storedProduct.get();
  }

  public Product getProductDetail(int id) {
    Product storedProduct = getProductById(id);
    String preSignedUrl = s3Service.getPreSignedUrl(storedProduct.getImageLocation());
    storedProduct.setImageLocation(preSignedUrl);

    return storedProduct;
  }

  public Product createProduct(Product product, MultipartFile productImage) throws IOException {
    Product storedProduct = productRepository.save(product);
    if (!productImage.isEmpty()) {
      String filePath = String.format("%s-%s", storedProduct.getId(),
          productImage.getOriginalFilename());
      storedProduct.setImageLocation(filePath);
      productRepository.save(storedProduct);

      String preSignedUrl = s3Service.uploadFile(filePath, productImage);
      storedProduct.setImageLocation(preSignedUrl);
    }

    return storedProduct;
  }

  public List<Product> getProducts() {
    List<Product> productList = productRepository.findAll();
    for (Product product : productList) {
      String preSignedUrl = s3Service.getPreSignedUrl(product.getImageLocation());
      product.setImageLocation(preSignedUrl);
    }

    return productList;
  }

  public Product updateProduct(Product product, MultipartFile productImage) throws IOException {
    Product productToUpdate = this.getProductById(product.getId());
    String imageLocation = productToUpdate.getImageLocation();
    String preSignedImageUrl = null;

    productToUpdate.updateProduct(product);
    s3Service.deleteFile(imageLocation);

    if (productImage.isEmpty()) {
      productToUpdate.setImageLocation(null);
    } else {
      imageLocation = String.format("%s-%s", productToUpdate.getId(),
          productImage.getOriginalFilename());
      preSignedImageUrl = s3Service.uploadFile(imageLocation, productImage);
      productToUpdate.setImageLocation(imageLocation);
    }

    productRepository.save(productToUpdate);
    productToUpdate.setImageLocation(preSignedImageUrl);

    return productToUpdate;
  }

  public void deleteProduct(int id) throws ProductNotFoundException {
    Product productToDelete = getProductById(id);
    productRepository.deleteById(id);
    s3Service.deleteFile(productToDelete.getImageLocation());
  }
}

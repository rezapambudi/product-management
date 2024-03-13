package com.sample.productsmanagement.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private String name;
  private int price;
  private int quantity;

  private String imageLocation;

  public void updateProduct(Product updatedProduct) {
    this.name = updatedProduct.name;
    this.price = updatedProduct.price;
    this.quantity = updatedProduct.quantity;
  }

  public ProductDTO convertToDTO() {
    return ProductDTO.builder()
        .id(this.id)
        .name(this.name)
        .price(this.price)
        .quantity(this.quantity)
        .imageUrl(this.imageLocation)
        .build();
  }
}

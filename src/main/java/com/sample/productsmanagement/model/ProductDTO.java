package com.sample.productsmanagement.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
  private int id;

  @NotEmpty(message = "Product name should not be empty")
  @Size(min = 5, message = "Minimum product name length is 5")
  private String name;

  @Min(value = 1, message = "Minimum product price is 1")
  private int price;

  @Min(value = 0, message = "Product quantity should not less than 0")
  private int quantity;

  private String imageUrl;

  public Product mapToProduct(){
    return Product.builder()
        .name(this.name)
        .price(this.price)
        .quantity(this.quantity)
        .build();
  }
}

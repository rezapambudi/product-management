package com.sample.productsmanagement.exception;

public class ProductNotFoundException extends RuntimeException{
  public ProductNotFoundException(String message) {
    super(message);
  }
}

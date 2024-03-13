package com.sample.productsmanagement.controller;

import com.sample.productsmanagement.exception.InvalidFileFormatException;
import com.sample.productsmanagement.exception.ProductNotFoundException;
import java.util.HashMap;
import java.util.List;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerAdvisor {
  private HashMap<String, String> createErrorMessage(String messageContent) {
    HashMap<String, String> message = new HashMap<>();
    message.put("message", messageContent);
    return message;
  }

  @ExceptionHandler(value = ProductNotFoundException.class)
  public ResponseEntity<Object> handleProductNotFoundException(ProductNotFoundException exception){
    return new ResponseEntity<>(createErrorMessage(exception.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(value = MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleInvalidRequest(MethodArgumentNotValidException exception){
    List<ObjectError> violationLists = exception.getBindingResult().getAllErrors();
    List<String> errorList = violationLists.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
    String errorMessage = String.join(", ", errorList);

    return new ResponseEntity<>(createErrorMessage(errorMessage), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(value = InvalidFileFormatException.class)
  public ResponseEntity<Object> handleInvalidFileFormatException(InvalidFileFormatException exception){
    String message = String.format("Product image should be image format but got, %s", exception.getLocalizedMessage());

    return new ResponseEntity<>(createErrorMessage(message), HttpStatus.BAD_REQUEST);
  }
}

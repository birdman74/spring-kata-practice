package dev.brian.kata.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String error, String message, Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus httpStatus, String message) {
        return new ApiError(httpStatus.value(), httpStatus.getReasonPhrase(), message, null);
    }

    public static ApiError ofFields(HttpStatus httpStatus, String message, Map<String, String> fieldErrors) {
        return new ApiError(httpStatus.value(), httpStatus.getReasonPhrase(), message, fieldErrors);
    }
}

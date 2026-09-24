package dev.kauanallyson.images.controller.advice;

import dev.kauanallyson.images.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public final class GlobalExceptionHandler {

    private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail businessException(BusinessException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Business failure [{}]: {}", ex.getStatus(), ex.getMessage(), ex);
        } else {
            log.warn("Business rule violated [{}]: {}", ex.getStatus(), ex.getMessage());
        }
        return problem(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail missingRequestHeaderException(MissingRequestHeaderException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Missing required header: " + ex.getHeaderName(), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail missingServletRequestPartException(MissingServletRequestPartException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Missing required multipart part: " + ex.getRequestPartName(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail maxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "Uploaded file exceeds the maximum allowed size", request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ProblemDetail methodArgumentTypeMismatchException(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request parameter: " + ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail checkedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }
}

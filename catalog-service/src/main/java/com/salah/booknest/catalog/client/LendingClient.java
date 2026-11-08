package com.salah.booknest.catalog.client;

import com.salah.booknest.catalog.exception.InformationNotFoundException;
import com.salah.booknest.catalog.exception.InvalidRequestException;
import com.salah.booknest.catalog.exception.InvalidStateException;
import com.salah.booknest.catalog.exception.ServiceUnavailableException;
import com.salah.booknest.catalog.model.response.ApiError;
import com.salah.booknest.catalog.model.response.StockResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Calls lending-service, which owns the copy counts and the loans.
 * The caller's token is sent along, so lending-service applies the same role checks.
 */
@Slf4j
@Component
public class LendingClient {

    private final RestClient restClient;

    public LendingClient(@Value("${lending-service.url}") String lendingUrl) {
        this.restClient = RestClient.builder().baseUrl(lendingUrl).build();
    }

    /** Copy counts by book id. Books without stock are missing from the map. */
    public Map<Long, StockResponse> getStock(Collection<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        String ids = bookIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<StockResponse> stock = call(() -> restClient.get()
                .uri("/api/inventory?bookIds={ids}", ids)
                .header(HttpHeaders.AUTHORIZATION, callerToken())
                .retrieve()
                .body(new ParameterizedTypeReference<List<StockResponse>>() { }));
        return stock.stream().collect(Collectors.toMap(StockResponse::bookId, Function.identity()));
    }

    public void createStock(Long bookId, int totalCopies) {
        call(() -> restClient.post()
                .uri("/api/inventory")
                .header(HttpHeaders.AUTHORIZATION, callerToken())
                .body(Map.of("bookId", bookId, "totalCopies", totalCopies))
                .retrieve()
                .toBodilessEntity());
    }

    public void changeTotalCopies(Long bookId, int totalCopies) {
        call(() -> restClient.put()
                .uri("/api/inventory/{bookId}", bookId)
                .header(HttpHeaders.AUTHORIZATION, callerToken())
                .body(Map.of("totalCopies", totalCopies))
                .retrieve()
                .toBodilessEntity());
    }

    /** Refused by lending-service while a copy is on loan or a return is waiting for review. */
    public void retire(Long bookId) {
        call(() -> restClient.delete()
                .uri("/api/inventory/{bookId}", bookId)
                .header(HttpHeaders.AUTHORIZATION, callerToken())
                .retrieve()
                .toBodilessEntity());
    }

    /** True if the logged-in member returned this book (needed to review it). */
    public boolean hasReturned(Long bookId) {
        return Boolean.TRUE.equals(call(() -> restClient.get()
                .uri("/api/loans/returned/{bookId}", bookId)
                .header(HttpHeaders.AUTHORIZATION, callerToken())
                .retrieve()
                .body(Boolean.class)));
    }

    /** Turns lending-service errors into this service's own errors, keeping its message. */
    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            ApiError error = e.getResponseBodyAs(ApiError.class);
            String message = error != null && error.message() != null ? error.message() : e.getStatusText();
            switch (e.getStatusCode().value()) {
                case 400 -> throw new InvalidRequestException(message);
                case 404 -> throw new InformationNotFoundException(message);
                case 409 -> throw new InvalidStateException(message, error != null ? error.error() : "INVALID_STATE");
                default -> {
                    log.error("lending-service answered {}: {}", e.getStatusCode().value(), message);
                    throw new ServiceUnavailableException("The lending service could not handle the request");
                }
            }
        } catch (ResourceAccessException e) {
            log.error("lending-service is not reachable: {}", e.getMessage());
            throw new ServiceUnavailableException("The lending service is not available. Please try again later");
        }
    }

    private String callerToken() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}

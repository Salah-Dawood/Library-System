package com.salah.booknest.util;

import com.salah.booknest.exception.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Builds the Pageable used by every paged list (books, loans, users, audit log),
 * so they all follow the same rules.
 */
public final class PageUtils {

    public static final int MAX_PAGE_SIZE = 100;

    private PageUtils() {
    }

    /**
     * Page numbers below 0 become 0 and the page size is kept between 1 and 100.
     * Anything other than "asc" sorts descending. Rows with the same value are ordered by id,
     * so a row can never show up on two pages.
     *
     * @throws InvalidRequestException if sortBy is not one of the allowed fields
     */
    public static Pageable pageable(int page, int size, String sortBy, String sortDir, Set<String> sortableFields) {
        if (!sortableFields.contains(sortBy)) {
            throw new InvalidRequestException("Cannot sort by '" + sortBy + "'. Use one of " + sortableFields);
        }
        Sort.Direction direction = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        if (!sortBy.equals("id")) {
            sort = sort.and(Sort.by(direction, "id"));
        }
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }
}

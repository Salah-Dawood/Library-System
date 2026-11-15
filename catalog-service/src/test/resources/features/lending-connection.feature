Feature: Talking to lending-service
  As a member or librarian
  I want catalog-service to stay predictable when lending-service has a problem
  So that I get a clear answer instead of a hanging page or a book without copies

  catalog-service calls lending-service over HTTP (REST) for the copy counts, passing on the caller's token.

  Background:
    Given the catalogue has the author "Frank Herbert" and the genre "Sci-Fi"

  Scenario: The caller's token is passed on to lending-service
    Given the librarian has added the book "Dune" with 3 copies
    And I am logged in as the member "janed"
    When I open the book "Dune"
    Then the response status is 200
    And lending-service was asked with the token of "janed"

  Scenario: lending-service is down while a book is added
    Given lending-service is down
    And I am logged in as the librarian
    When I add the book "Dune" with 3 copies
    Then the response status is 503
    And the error is "SERVICE_UNAVAILABLE"
    And the catalogue has no book called "Dune"

  Scenario: lending-service fails while a book is added
    Given lending-service fails with an internal error
    And I am logged in as the librarian
    When I add the book "Dune" with 3 copies
    Then the response status is 503
    And the error is "SERVICE_UNAVAILABLE"
    And the catalogue has no book called "Dune"

  Scenario: The book list answers 503 while lending-service is down, and works again once it is back
    Given the librarian has added the book "Dune" with 3 copies
    And I am logged in as the member "johnd"
    And lending-service is down
    When I search the books for "dune"
    Then the response status is 503
    And the error is "SERVICE_UNAVAILABLE"
    When lending-service is back
    And I search the books for "dune"
    Then the response status is 200
    And the list shows "Dune" with 3 of 3 copies on the shelf

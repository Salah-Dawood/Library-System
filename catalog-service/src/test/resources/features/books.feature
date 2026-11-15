Feature: Books in the catalogue
  As a librarian
  I want to add, change and remove books
  So that members always see what the library has, with the copy counts that lending-service keeps

  Background:
    Given the catalogue has the author "Frank Herbert" and the genre "Sci-Fi"

  Scenario: A librarian adds a book and lending-service creates its copies
    Given I am logged in as the librarian
    When I add the book "Dune" with 3 copies
    Then the response status is 201
    And lending-service holds 3 copies of "Dune"
    And the book shows 3 copies, 3 on the shelf

  Scenario: A member cannot add books
    Given I am logged in as the member "johnd"
    When I add the book "Dune" with 3 copies
    Then the response status is 403
    And lending-service was not asked to create any copies

  Scenario: Two books cannot share an ISBN
    Given the librarian has added the book "Dune" with ISBN "9780441172719" and 3 copies
    And I am logged in as the librarian
    When I add the book "Dune again" with ISBN "9780441172719" and 1 copies
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"

  Scenario: A book needs at least one genre
    Given I am logged in as the librarian
    When I add the book "Dune" without any genre
    Then the response status is 400
    And the error is "VALIDATION_FAILED"
    And lending-service was not asked to create any copies

  Scenario: The book list shows the copy counts from lending-service
    Given the librarian has added the book "Dune" with 3 copies
    And the librarian has added the book "Dune Messiah" with 2 copies
    And 1 copies of "Dune" are on loan
    And I am logged in as the member "johnd"
    When I search the books for "dune"
    Then the response status is 200
    And the list shows "Dune" with 2 of 3 copies on the shelf
    And the list shows "Dune Messiah" with 2 of 2 copies on the shelf

  Scenario: Changing the number of copies is passed on to lending-service
    Given the librarian has added the book "Dune" with 3 copies
    And I am logged in as the librarian
    When I change the copies of "Dune" to 5
    Then the response status is 200
    And lending-service holds 5 copies of "Dune"
    And the book shows 5 copies, 5 on the shelf

  Scenario: The copies cannot go below the ones on loan
    Given the librarian has added the book "Dune" with 3 copies
    And 2 copies of "Dune" are on loan
    And I am logged in as the librarian
    When I change the copies of "Dune" to 1
    Then the response status is 400
    And lending-service holds 3 copies of "Dune"

  Scenario: A librarian removes a book
    Given the librarian has added the book "Dune" with 3 copies
    And I am logged in as the librarian
    When I remove the book "Dune"
    Then the response status is 204
    And lending-service holds no copies of "Dune"
    When I open the book "Dune"
    Then the response status is 404

  Scenario: A book with a copy on loan cannot be removed
    Given the librarian has added the book "Dune" with 3 copies
    And 1 copies of "Dune" are on loan
    And I am logged in as the librarian
    When I remove the book "Dune"
    Then the response status is 409
    And the error is "BOOK_ON_LOAN"
    When I open the book "Dune"
    Then the response status is 200

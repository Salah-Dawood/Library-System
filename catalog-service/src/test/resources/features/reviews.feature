Feature: Reviews
  As a member
  I want to review the books I have read
  So that other members know which books are worth borrowing

  Only a member who returned the book can review it (catalog-service asks lending-service), once per book.

  Background:
    Given the catalogue has the author "Frank Herbert" and the genre "Sci-Fi"
    And the librarian has added the book "Dune" with 3 copies

  Scenario: A member who returned the book can review it
    Given "johnd" has returned "Dune"
    When "johnd" reviews "Dune" with 9 out of 10 saying "A classic"
    Then the response status is 201
    When "janed" opens the reviews of "Dune"
    Then the book has 1 review with an average of 9.0

  Scenario: A member who never returned the book cannot review it
    When "janed" reviews "Dune" with 9 out of 10 saying "Looks nice"
    Then the response status is 409

  Scenario: A member reviews a book only once
    Given "johnd" has reviewed "Dune" with 8 out of 10
    When "johnd" reviews "Dune" with 10 out of 10 saying "Even better the second time"
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"

  Scenario Outline: The rating is from 1 to 10
    Given "johnd" has returned "Dune"
    When "johnd" reviews "Dune" with <rating> out of 10
    Then the response status is <status>

    Examples:
      | rating | status |
      | 0      | 400    |
      | 1      | 201    |
      | 10     | 201    |
      | 11     | 400    |

  Scenario: The average rating covers every review
    Given "johnd" has reviewed "Dune" with 8 out of 10
    And "alexm" has reviewed "Dune" with 10 out of 10
    When "janed" opens the reviews of "Dune"
    Then the book has 2 reviews with an average of 9.0

  Scenario: A member is offered to review only a returned book they have not reviewed yet
    Given "johnd" has returned "Dune"
    When "johnd" opens the reviews of "Dune"
    Then they are offered to write a review
    When "johnd" reviews "Dune" with 7 out of 10
    And "johnd" opens the reviews of "Dune"
    Then they are not offered to write a review
    When "janed" opens the reviews of "Dune"
    Then they are not offered to write a review

  Scenario: A member can change their own review
    Given "johnd" has reviewed "Dune" with 4 out of 10
    When "johnd" changes the review by "johnd" to 8 out of 10
    Then the response status is 200
    When "janed" opens the reviews of "Dune"
    Then the book has 1 review with an average of 8.0

  Scenario: A member cannot change someone else's review
    Given "johnd" has reviewed "Dune" with 9 out of 10
    When "alexm" changes the review by "johnd" to 1 out of 10
    Then the response status is 403

  Scenario: A member cannot delete someone else's review
    Given "johnd" has reviewed "Dune" with 9 out of 10
    When "alexm" deletes the review by "johnd"
    Then the response status is 403

  Scenario: A librarian can delete any review
    Given "johnd" has reviewed "Dune" with 9 out of 10
    When the librarian deletes the review by "johnd"
    Then the response status is 204
    When "janed" opens the reviews of "Dune"
    Then the book has no reviews

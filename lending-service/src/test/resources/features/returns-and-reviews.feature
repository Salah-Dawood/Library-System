Feature: Returns, reviews and cancelling
  As a member I want to return my books, and as a librarian I want to check every returned copy
  So that late returns are fined and damaged copies leave the shelf

  A returned copy waits for a librarian's review (PENDING_REVIEW) before it goes back on the shelf.

  Background:
    Given the library has 3 copies of "Clean Code"

  Scenario: A late return waits for review and gets a late fine
    Given "alice" has borrowed "Clean Code", due 3 days ago
    When "alice" returns "Clean Code"
    Then the response status is 200
    And the loan is "PENDING_REVIEW"
    And the return was "LATE"
    And it was 3 days late
    And "alice" has a "LATE" fine of 1.05
    And "Clean Code" has 2 of 3 copies on the shelf

  Scenario: An early return waits for review without a fine
    Given "alice" has borrowed "Clean Code", due in 2 days
    When "alice" returns "Clean Code"
    Then the response status is 200
    And the loan is "PENDING_REVIEW"
    And the return was "EARLY"
    And "alice" has no fines

  Scenario: A member cannot return someone else's book
    Given "alice" has borrowed "Clean Code"
    When "bob" returns the loan of "Clean Code" by "alice"
    Then the response status is 403

  Scenario: A book cannot be returned twice
    Given "alice" has borrowed "Clean Code"
    And "alice" has returned "Clean Code"
    When "alice" returns "Clean Code"
    Then the response status is 409

  Scenario: A copy in good condition goes back on the shelf after the review
    Given "alice" has borrowed "Clean Code"
    And "alice" has returned "Clean Code"
    When the librarian reviews the return of "Clean Code" by "alice" as not damaged
    Then the response status is 200
    And the loan is "RETURNED"
    And the return was reviewed by the librarian
    And "Clean Code" has 3 of 3 copies on the shelf

  Scenario: A damaged copy is taken out of circulation
    Given "alice" has borrowed "Clean Code"
    And "alice" has returned "Clean Code"
    When the librarian reviews the return of "Clean Code" by "alice" as damaged
    Then the response status is 200
    And the loan is "RETURNED"
    And "Clean Code" has 2 of 2 copies on the shelf

  Scenario: Only a returned copy can be reviewed
    Given "alice" has borrowed "Clean Code"
    When the librarian reviews the return of "Clean Code" by "alice" as not damaged
    Then the response status is 409

  Scenario: A librarian cancels a loan and the copy goes back on the shelf
    Given "alice" has borrowed "Clean Code"
    When the librarian cancels the loan of "Clean Code" by "alice"
    Then the response status is 200
    And the loan is "CANCELLED"
    And "Clean Code" has 3 of 3 copies on the shelf

  Scenario: A returned loan cannot be cancelled
    Given "alice" has borrowed "Clean Code"
    And "alice" has returned "Clean Code"
    And the librarian reviews the return of "Clean Code" by "alice" as not damaged
    When the librarian cancels the loan of "Clean Code" by "alice"
    Then the response status is 409

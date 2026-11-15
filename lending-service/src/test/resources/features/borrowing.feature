Feature: Borrowing books
  As a member
  I want to borrow books for a few days
  So that I can read them at home, within the library's rules

  Rules: at most 30 days, one copy of the same book at a time, at most 5 books on loan,
  and only while a copy is on the shelf.

  Background:
    Given the library has 3 copies of "Clean Code"

  Scenario: A member borrows a book
    When "alice" borrows "Clean Code" for 14 days
    Then the response status is 201
    And the loan is "BORROWED"
    And the loan belongs to "alice"
    And the loan starts today and is due in 14 days
    And "Clean Code" has 2 of 3 copies on the shelf
    And the audit log records "BORROWED" for the loan of "Clean Code" by "alice"

  Scenario: A loan is at most 30 days
    When "alice" borrows "Clean Code" for 31 days
    Then the response status is 400
    And no loan of "Clean Code" was created
    And "Clean Code" has 3 of 3 copies on the shelf

  Scenario: Borrowing needs a login
    When someone borrows "Clean Code" without logging in
    Then the response status is 401
    And "Clean Code" has 3 of 3 copies on the shelf

  Scenario: The book must be named
    When "alice" borrows without saying which book
    Then the response status is 400

  Scenario: A member cannot borrow the same book twice
    Given "alice" has borrowed "Clean Code"
    When "alice" borrows "Clean Code"
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"
    And "Clean Code" has 2 of 3 copies on the shelf

  Scenario: A member has at most 5 books on loan
    Given "alice" already has 5 other books on loan
    When "alice" borrows "Clean Code"
    Then the response status is 409
    And the error is "LOAN_LIMIT_REACHED"
    And no loan of "Clean Code" was created

  Scenario: No copy is left on the shelf
    Given the library has 1 copies of "Refactoring"
    And "bob" has borrowed "Refactoring"
    When "alice" borrows "Refactoring"
    Then the response status is 409
    And the error is "NO_COPIES_AVAILABLE"
    And "Refactoring" has 0 of 1 copies on the shelf

  Scenario: A book that is no longer lent out cannot be borrowed
    Given "Clean Code" is no longer lent out
    When "alice" borrows "Clean Code"
    Then the response status is 409
    And no loan of "Clean Code" was created

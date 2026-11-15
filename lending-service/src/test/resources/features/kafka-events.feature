Feature: Events published to Kafka
  As notification-service
  I want to hear about every change to a member's loans and holds
  So that I can email them and show it live, without lending-service ever calling me

  lending-service publishes to loan-events and waitlist-events only after the database change is saved,
  so nothing is ever announced for a change that was rolled back.

  Background:
    Given the library has 1 copies of "Clean Code"

  Scenario: A member's return tells the librarians that a copy waits for review
    Given "alice" has borrowed "Clean Code"
    When "alice" returns "Clean Code"
    Then the librarians are told "LOAN_RETURNED" about the loan of "Clean Code" by "alice"

  Scenario: A return recorded by a librarian tells the member
    Given "alice" has borrowed "Clean Code"
    When the librarian records the return of "Clean Code" by "alice"
    Then "alice" is told "LOAN_RETURNED" about their loan of "Clean Code"

  Scenario: The librarian's review is announced to the member
    Given "alice" has borrowed "Clean Code"
    And "alice" has returned "Clean Code"
    When the librarian reviews the return of "Clean Code" by "alice" as not damaged
    Then "alice" is told "RETURN_REVIEWED" about their loan of "Clean Code"

  Scenario: A cancelled loan is announced to the member
    Given "alice" has borrowed "Clean Code"
    When the librarian cancels the loan of "Clean Code" by "alice"
    Then "alice" is told "LOAN_CANCELLED" about their loan of "Clean Code"

  Scenario: A refused return announces nothing more
    Given "alice" has borrowed "Clean Code"
    And "alice" has returned "Clean Code"
    When "alice" returns "Clean Code"
    Then the response status is 409
    And the librarians are told "LOAN_RETURNED" only once about the loan of "Clean Code" by "alice"

  Scenario: A copy held for a member is announced on the waitlist topic
    Given "bob" has borrowed "Clean Code"
    And "alice" is on the waitlist of "Clean Code"
    When "bob" returns "Clean Code" in good condition
    Then "alice" is told "HOLD_READY" about the waitlist of "Clean Code"

  Scenario: A hold that runs out is announced
    Given "bob" has borrowed "Clean Code"
    And "alice" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    And the hold of "alice" on "Clean Code" has run out
    When the hold expiry job runs
    Then "alice" is told "HOLD_EXPIRED" about the waitlist of "Clean Code"

  Scenario: The due reminder job reminds only the members whose book is due tomorrow
    Given the library has 1 copies of "Refactoring"
    And "alice" has borrowed "Clean Code", due in 1 days
    And "bob" has borrowed "Refactoring", due in 3 days
    When the due reminder job runs
    Then "alice" is reminded that "Clean Code" is due tomorrow
    And "bob" is not reminded about "Refactoring"

  Scenario: Nothing is announced for a change that was rolled back
    When a change that announces "LOAN_CANCELLED" to "alice" is rolled back
    And a change that announces "RETURN_REVIEWED" to "alice" is saved
    Then "alice" is told "RETURN_REVIEWED" but never "LOAN_CANCELLED"

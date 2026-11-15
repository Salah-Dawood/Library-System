Feature: Many requests at the same moment
  As the library
  I want the rules to hold even when many people click at the same instant
  So that a copy is never lent twice, a limit is never passed and nothing is paid or counted twice

  Every "at the same moment" step releases all its requests together against the real REST API.
  The database locks (member first, then the book) decide who wins, so this also holds with several
  lending-service instances running behind the load balancer.

  # ---------- borrowing ----------

  Scenario: 10 members race for the last copy, only one gets it
    Given the library has 1 copies of "Clean Code"
    When 10 different members try to borrow "Clean Code" at the same moment
    Then exactly 1 of them succeeded
    And 9 of them were refused with "NO_COPIES_AVAILABLE"
    And 1 member has "Clean Code" on loan
    And "Clean Code" has 0 of 1 copies on the shelf
    And the shelf count of "Clean Code" adds up

  Scenario: 10 members race for 3 copies, exactly 3 get one
    Given the library has 3 copies of "Clean Code"
    When 10 different members try to borrow "Clean Code" at the same moment
    Then exactly 3 of them succeeded
    And 7 of them were refused with "NO_COPIES_AVAILABLE"
    And "Clean Code" has 0 of 3 copies on the shelf
    And the shelf count of "Clean Code" adds up

  Scenario: A member who double-clicks gets only one loan
    Given the library has 5 copies of "Clean Code"
    When "alice" tries to borrow "Clean Code" 5 times at the same moment
    Then exactly 1 of them succeeded
    And 4 of them were refused with "RESOURCE_ALREADY_EXISTS"
    And "Clean Code" has 4 of 5 copies on the shelf
    And the shelf count of "Clean Code" adds up

  Scenario: The loan limit holds when a member borrows several books at once
    Given "alice" already has 4 other books on loan
    When "alice" tries to borrow 4 different books at the same moment
    Then exactly 1 of them succeeded
    And 3 of them were refused with "LOAN_LIMIT_REACHED"

  Scenario: No deadlock when members borrow and queue for the same books in different orders
    Given the library has 2 copies of "Clean Code"
    And the library has 2 copies of "Refactoring"
    And the library has 2 copies of "Domain-Driven Design"
    When 6 members borrow "Clean Code", "Refactoring" and "Domain-Driven Design" and queue for them, in different orders, at the same moment
    Then no request failed with a server error
    And every request was answered within 20 seconds
    And nobody has more than 5 books on loan
    And the shelf count of "Clean Code" adds up
    And the shelf count of "Refactoring" adds up
    And the shelf count of "Domain-Driven Design" adds up

  # ---------- returns, reviews and fines ----------

  Scenario: A return and a cancel at the same moment, only one of them wins
    Given the library has 1 copies of "Clean Code"
    And "alice" has borrowed "Clean Code"
    When "alice" returns "Clean Code" while the librarian cancels it, at the same moment
    Then exactly 1 of them succeeded
    And the shelf count of "Clean Code" adds up

  Scenario: A member returns the same book 4 times at once, it counts once
    Given the library has 1 copies of "Clean Code"
    And "alice" has borrowed "Clean Code"
    When "alice" returns "Clean Code" 4 times at the same moment
    Then exactly 1 of them succeeded
    And the shelf count of "Clean Code" adds up

  Scenario: Two librarians review the same return, the copy goes back only once
    Given the library has 1 copies of "Clean Code"
    And "alice" has borrowed "Clean Code"
    And "alice" has returned "Clean Code"
    When two librarians review the return of "Clean Code" by "alice" at the same moment
    Then exactly 1 of them succeeded
    And "Clean Code" has 1 of 1 copies on the shelf
    And the shelf count of "Clean Code" adds up

  Scenario: A fine paid 5 times at once is paid only once
    Given "alice" has an unpaid fine
    When "alice" pays that fine 5 times at the same moment
    Then exactly 1 of them succeeded
    And 4 of them were refused with "FINE_ALREADY_PAID"
    And the fine is paid

  # ---------- waitlist and holds ----------

  Scenario: 8 members join the waitlist at once, each gets their own place
    Given the library has 1 copies of "Clean Code"
    And "bob" has borrowed "Clean Code"
    When 8 different members join the waitlist of "Clean Code" at the same moment
    Then everyone in line got a different place, from 1 to 8

  Scenario: A member who joins 5 times at once is in line only once
    Given the library has 1 copies of "Clean Code"
    And "bob" has borrowed "Clean Code"
    When "alice" joins the waitlist of "Clean Code" 5 times at the same moment
    Then exactly 1 of them succeeded
    And 4 of them were refused with "RESOURCE_ALREADY_EXISTS"

  Scenario: A held copy cannot be taken by walk-ins, even at the same moment
    Given the library has 1 copies of "Clean Code"
    And a copy of "Clean Code" is held for "alice"
    When 5 walk-in members try to borrow "Clean Code" while "alice" claims the held copy, at the same moment
    Then the held copy went to "alice"
    And exactly 1 of them succeeded
    And 5 of them were refused with "NO_COPIES_AVAILABLE"
    And the shelf count of "Clean Code" adds up

  Scenario: A hold claimed 5 times at once gives only one loan
    Given the library has 1 copies of "Clean Code"
    And a copy of "Clean Code" is held for "alice"
    When "alice" claims the held copy of "Clean Code" 5 times at the same moment
    Then exactly 1 of them succeeded
    And 4 of them were refused with "NO_ACTIVE_HOLD"
    And the shelf count of "Clean Code" adds up

  Scenario: A hold expired 5 times at once offers the copy only once
    Given the library has 1 copies of "Clean Code"
    And a copy of "Clean Code" was held for "alice" but the hold has run out
    When the expiry of the hold of "alice" on "Clean Code" runs 5 times at the same moment
    Then exactly 1 of them expired the hold
    And the place of "alice" on the waitlist of "Clean Code" is "EXPIRED"
    And "Clean Code" has 1 of 1 copies on the shelf
    And the shelf count of "Clean Code" adds up

  Scenario: Giving up a hold while it expires offers the copy only once
    Given the library has 1 copies of "Clean Code"
    And a copy of "Clean Code" was held for "alice" but the hold has run out
    When "alice" gives up the held copy of "Clean Code" while the hold expires, at the same moment
    Then "Clean Code" has 1 of 1 copies on the shelf
    And the shelf count of "Clean Code" adds up

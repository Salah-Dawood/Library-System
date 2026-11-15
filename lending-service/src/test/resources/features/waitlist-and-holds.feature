Feature: Waitlist and holds
  As a member
  I want to queue for a book that is out and get the next copy that comes back
  So that I do not have to keep checking the shelf

  A returned copy is first offered to the first member in line: it is held for them for 24 hours.
  If they do not claim it in time, the hold expiry job (every minute) passes the copy on.

  Background:
    Given the library has 1 copies of "Clean Code"
    And "bob" has borrowed "Clean Code"

  # ---------- joining ----------

  Scenario: A member joins the waitlist of a book that is out
    When "alice" joins the waitlist of "Clean Code"
    Then the response status is 201
    And "alice" is number 1 in line

  Scenario: The second member in line is number 2
    Given "alice" is on the waitlist of "Clean Code"
    When "carol" joins the waitlist of "Clean Code"
    Then "carol" is number 2 in line

  Scenario: There is no waitlist while a copy is on the shelf
    Given the library has 2 copies of "Refactoring"
    When "alice" joins the waitlist of "Refactoring"
    Then the response status is 409
    And the error is "COPIES_AVAILABLE"

  Scenario: A member is in line only once
    Given "alice" is on the waitlist of "Clean Code"
    When "alice" joins the waitlist of "Clean Code"
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"

  Scenario: The member who has the book cannot queue for it
    When "bob" joins the waitlist of "Clean Code"
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"

  Scenario: A member waits for at most 5 books
    Given "alice" is already waiting for 5 other books
    When "alice" joins the waitlist of "Clean Code"
    Then the response status is 409
    And the error is "WAITLIST_LIMIT_REACHED"

  # ---------- a copy comes back ----------

  Scenario: With nobody waiting, a returned copy goes back on the shelf
    When "bob" returns "Clean Code" in good condition
    Then "Clean Code" has 1 of 1 copies on the shelf

  Scenario: A returned copy is held for the first in line, not put on the shelf
    Given "alice" is on the waitlist of "Clean Code"
    And "carol" is on the waitlist of "Clean Code"
    When "bob" returns "Clean Code" in good condition
    Then the copy of "Clean Code" is held for "alice" for 24 hours
    And the place of "carol" on the waitlist of "Clean Code" is "WAITING"
    And "Clean Code" has 0 of 1 copies on the shelf
    And the shelf count of "Clean Code" adds up

  # ---------- claiming ----------

  Scenario: The member claims the held copy and gets the loan
    Given "alice" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    When "alice" claims the held copy of "Clean Code"
    Then the response status is 201
    And the loan is "BORROWED"
    And the place of "alice" on the waitlist of "Clean Code" is "FULFILLED"
    And the audit log records "CLAIMED" for the hold of "alice" on "Clean Code"
    And the shelf count of "Clean Code" adds up

  Scenario: A hold that ran out cannot be claimed
    Given "alice" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    And the hold of "alice" on "Clean Code" has run out
    When "alice" claims the held copy of "Clean Code"
    Then the response status is 409
    And the error is "HOLD_EXPIRED"

  Scenario: There is nothing to claim while the member is still waiting
    Given "alice" is on the waitlist of "Clean Code"
    When "alice" claims the held copy of "Clean Code"
    Then the response status is 409
    And the error is "NO_ACTIVE_HOLD"

  Scenario: A hold is claimed only once
    Given "alice" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    And "alice" claims the held copy of "Clean Code"
    When "alice" claims the held copy of "Clean Code"
    Then the response status is 409
    And the error is "NO_ACTIVE_HOLD"

  Scenario: Nobody else can claim a copy held for a member
    Given "alice" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    When "carol" claims the held copy of "Clean Code" that is kept for "alice"
    Then the response status is 403
    And the copy of "Clean Code" is held for "alice" for 24 hours

  Scenario: The loan limit also applies to a held copy
    Given "alice" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    And "alice" already has 5 other books on loan
    When "alice" claims the held copy of "Clean Code"
    Then the response status is 409
    And the error is "LOAN_LIMIT_REACHED"
    And the copy of "Clean Code" is held for "alice" for 24 hours

  Scenario Outline: A held copy is claimed for 1 to 30 days
    Given "alice" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    When "alice" claims the held copy of "Clean Code" for <days> days
    Then the response status is <status>

    Examples:
      | days | status |
      | 0    | 400    |
      | 1    | 201    |
      | 30   | 201    |
      | 31   | 400    |

  # ---------- leaving ----------

  Scenario: A member leaves the waitlist
    Given "alice" is on the waitlist of "Clean Code"
    When "alice" leaves the waitlist of "Clean Code"
    Then the place of "alice" on the waitlist of "Clean Code" is "CANCELLED"

  Scenario: A member who gives up a held copy passes it on
    Given "alice" is on the waitlist of "Clean Code"
    And "carol" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    When "alice" leaves the waitlist of "Clean Code"
    Then the place of "alice" on the waitlist of "Clean Code" is "CANCELLED"
    And the copy of "Clean Code" is held for "carol" for 24 hours
    And the shelf count of "Clean Code" adds up

  Scenario: A member who gives up a held copy with nobody behind them puts it back on the shelf
    Given "alice" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    When "alice" leaves the waitlist of "Clean Code"
    Then "Clean Code" has 1 of 1 copies on the shelf

  Scenario: A member cannot remove someone else's place
    Given "alice" is on the waitlist of "Clean Code"
    When "carol" removes the place of "alice" on the waitlist of "Clean Code"
    Then the response status is 403
    And the place of "alice" on the waitlist of "Clean Code" is "WAITING"

  Scenario: A place that was left cannot be left again
    Given "alice" is on the waitlist of "Clean Code"
    And "alice" leaves the waitlist of "Clean Code"
    When "alice" leaves the waitlist of "Clean Code"
    Then the response status is 409

  # ---------- the expiry job ----------

  Scenario: The expiry job ends every hold that ran out and puts the copies back
    Given 6 members each have a hold that ran out
    And another member has a hold that ends in an hour
    When the hold expiry job runs
    Then all those holds have expired and their copies are back on the shelf
    And the hold that ends in an hour is still waiting to be claimed

  Scenario: An expired hold passes the copy to the next in line
    Given "alice" is on the waitlist of "Clean Code"
    And "carol" is on the waitlist of "Clean Code"
    And "bob" returns "Clean Code" in good condition
    And the hold of "alice" on "Clean Code" has run out
    When the hold expiry job runs
    Then the place of "alice" on the waitlist of "Clean Code" is "EXPIRED"
    And the copy of "Clean Code" is held for "carol" for 24 hours

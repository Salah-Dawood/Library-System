Feature: Notifications from Kafka events
  As a member
  I want an email when something happens to my loans and holds
  So that I do not miss a due date or a book that is waiting for me

  user-service and lending-service never call notification-service: they publish events to Kafka,
  and notification-service reacts to them on its own (asynchronous, decoupled).

  Background:
    Given "johnd" registered with the email "johnd@example.com"

  Scenario: A UserRegistered event gives notification-service the member's email
    When user-service announces that "alexm" registered with the email "alexm@example.com"
    Then notification-service knows that the email of "alexm" is "alexm@example.com"

  Scenario Outline: Each lending event sends the member the right email
    When lending-service announces "<type>" on "<topic>" for "johnd"
    Then "johnd@example.com" receives an email "<subject>"
    And the event is handled

    Examples:
      | type            | topic           | subject                                |
      | LOAN_CANCELLED  | loan-events     | BookNest - Your loan was cancelled     |
      | LOAN_RETURNED   | loan-events     | BookNest - We received your return     |
      | RETURN_REVIEWED | loan-events     | BookNest - Your return was reviewed    |
      | DUE_TOMORROW    | loan-events     | BookNest - Book due tomorrow           |
      | HOLD_READY      | waitlist-events | BookNest - A copy is ready for you     |
      | HOLD_EXPIRED    | waitlist-events | BookNest - Your hold expired           |

  Scenario: An event for the librarians is shown live but sends no email
    When lending-service announces "RETURN_NEEDS_REVIEW" for the librarians
    Then the event is handled
    And no email has been sent

  Scenario: A member without a known email does not block the event
    When lending-service announces "LOAN_RETURNED" on "loan-events" for "janed"
    Then the event is handled
    And no email has been sent

  Scenario: The same event delivered twice sends only one email
    When lending-service announces "LOAN_RETURNED" on "loan-events" for "johnd"
    Then "johnd@example.com" receives an email "BookNest - We received your return"
    When lending-service announces the same event again
    Then 1 email has been sent

Feature: Reliable event handling
  As the library
  I want a failing email to be tried again, and a hopeless event to be parked instead of lost
  So that members still get their emails and one bad event never blocks the ones behind it

  A failing event is tried again 3 times, 2 seconds apart. If it still fails it is moved to its
  dead-letter topic (<topic>.DLT). A message that is not valid JSON is moved there at once.

  Background:
    Given "johnd" registered with the email "johnd@example.com"

  Scenario: The mail server fails twice, then the retry delivers the email once
    Given the mail server fails the next 2 emails
    When lending-service announces "LOAN_RETURNED" on "loan-events" for "johnd"
    Then "johnd@example.com" receives an email "BookNest - We received your return"
    And sending the email was tried 3 times
    And the event is handled
    And 1 email has been sent

  Scenario: The mail server stays down, so the event is parked in the dead-letter topic
    Given the mail server is down
    When lending-service announces "HOLD_READY" on "waitlist-events" for "johnd"
    Then the event is moved to "waitlist-events.DLT"
    And it was tried again before being moved
    And sending the email was tried 4 times
    And the event is not marked as handled

  Scenario: A parked event does not block the events behind it
    Given the mail server is down
    When lending-service announces "LOAN_CANCELLED" on "loan-events" for "johnd"
    Then the event is moved to "loan-events.DLT"
    When the mail server is back
    And lending-service announces "DUE_TOMORROW" on "loan-events" for "johnd"
    Then "johnd@example.com" receives an email "BookNest - Book due tomorrow"
    And the event is handled

  Scenario: A message that is not JSON is parked at once, because trying again cannot fix it
    When a broken message that is not JSON arrives on "loan-events" for "johnd"
    Then the event is moved to "loan-events.DLT"
    And it was moved at once, without being tried again
    And sending the email was tried 0 times

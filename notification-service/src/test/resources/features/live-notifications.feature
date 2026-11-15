Feature: Live notifications
  As a member or librarian with BookNest open in my browser
  I want news about my loans to pop up straight away
  So that I do not have to reload the page

  The browser keeps a Server-Sent Events stream open; notification-service pushes into it
  when an event arrives from Kafka. The "Then" checks run once the event has been handled.

  Background:
    Given "johnd" registered with the email "johnd@example.com"

  Scenario: An open stream says it is connected
    When "janed" has the live notifications open
    Then "janed" sees a live "CONNECTED" notification

  Scenario: A member's event pops up in their own stream only
    Given "johnd" has the live notifications open
    And "janed" has the live notifications open
    When lending-service announces "HOLD_READY" on "waitlist-events" for "johnd"
    Then "johnd" sees a live "HOLD_READY" notification
    And the event is handled
    And "janed" sees no live "HOLD_READY" notification

  Scenario: An event for the librarians reaches the librarians, not the members
    Given "librarian" has the live notifications open
    And "johnd" has the live notifications open
    When lending-service announces "RETURN_NEEDS_REVIEW" for the librarians
    Then "librarian" sees a live "RETURN_NEEDS_REVIEW" notification
    And the event is handled
    And "johnd" sees no live "RETURN_NEEDS_REVIEW" notification

  Scenario: The pop-up comes only after the email went out, so a retry never shows it twice
    Given "johnd" has the live notifications open
    And the mail server fails the next 1 emails
    When lending-service announces "DUE_TOMORROW" on "loan-events" for "johnd"
    Then "johnd@example.com" receives an email "BookNest - Book due tomorrow"
    And "johnd" sees a live "DUE_TOMORROW" notification
    And the event is handled
    And "johnd" saw the "DUE_TOMORROW" notification once

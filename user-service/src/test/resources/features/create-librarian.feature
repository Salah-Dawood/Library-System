Feature: A librarian creates another librarian
  As a librarian
  I want to give a new colleague a librarian account myself
  So that they can start working straight away, without registering and waiting for an email code

  The new account is verified at once and starts with the password the creating librarian chose.

  Background:
    Given a librarian "lib" with password "Passw0rd!"
    And a verified member "alice" with password "Passw0rd!"
    And "lib" is logged in with password "Passw0rd!"

  Scenario: A librarian creates a librarian account
    When "lib" creates the librarian "sara" with email "sara@example.com" and password "Start123!"
    Then the response status is 201
    And the new account "sara" is a verified librarian
    And the response does not show the password

  Scenario: The new librarian can log in straight away and manage users
    Given "lib" creates the librarian "sara" with email "sara@example.com" and password "Start123!"
    When "sara" logs in with password "Start123!"
    Then the response status is 200
    And the login gives a token for "sara" with role "librarian"
    Given "sara" is logged in with password "Start123!"
    When "sara" lists the users
    Then the response status is 200
    And the list contains "sara" and "alice"

  Scenario: notification-service hears about the new librarian
    When "lib" creates the librarian "sara" with email "sara@example.com" and password "Start123!"
    Then the response status is 201
    And a UserRegistered event for "sara" with the email "sara@example.com" is published

  Scenario: A member cannot create librarians
    Given "alice" is logged in with password "Passw0rd!"
    When "alice" creates the librarian "sneaky" with email "sneaky@example.com" and password "Start123!"
    Then the response status is 403
    And there is no account "sneaky"

  Scenario: Creating a librarian needs a login
    When someone without a token creates the librarian "sneaky" with email "sneaky@example.com"
    Then the response status is 401
    And there is no account "sneaky"

  Scenario: The username is already taken
    When "lib" creates the librarian "alice" with email "other@example.com" and password "Start123!"
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"

  Scenario: The email address is already used
    When "lib" creates the librarian "sara" with email "alice@example.com" and password "Start123!"
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"
    And there is no account "sara"

  Scenario Outline: Invalid details are refused
    When "lib" creates a librarian with "<field>" set to "<value>"
    Then the response status is 400
    And the error is "VALIDATION_FAILED"
    And the field "<field>" is rejected

    Examples:
      | field        | value        |
      | username     | ab           |
      | emailAddress | not-an-email |
      | password     | short        |
      | age          | 12           |

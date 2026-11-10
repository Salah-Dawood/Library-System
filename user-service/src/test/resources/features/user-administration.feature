Feature: User administration
  As a librarian
  I want to see the members and switch accounts off and on
  So that I can keep the library safe

  Background:
    Given a librarian "lib" with password "Passw0rd!"
    And a verified member "alice" with password "Passw0rd!"
    And "lib" is logged in with password "Passw0rd!"

  Scenario: The librarian lists the users
    When "lib" lists the users
    Then the response status is 200
    And the list contains "lib" and "alice"

  Scenario: The librarian opens one user
    When "lib" opens the account of "alice"
    Then the response status is 200
    And the account shows username "alice" with email "alice@example.com"
    And the account does not show the password

  Scenario: Opening a user that does not exist
    When "lib" opens the account with id 999999
    Then the response status is 404
    And the error is "RESOURCE_NOT_FOUND"

  Scenario: A deactivated member cannot log in any more
    When "lib" deactivates "alice"
    Then the response status is 200
    And "alice" is shown as not active
    And "alice" cannot log in because the account is deactivated

  Scenario: A deactivated member's token stops working at once
    Given "alice" is logged in with password "Passw0rd!"
    When "lib" deactivates "alice"
    And "alice" opens their profile
    Then the response status is 401

  Scenario: An activated member can log in again
    Given "lib" deactivated "alice"
    When "lib" activates "alice"
    Then the response status is 200
    And "alice" can log in with password "Passw0rd!"

  Scenario: A librarian cannot deactivate their own account
    When "lib" deactivates "lib"
    Then the response status is 409
    And the error is "INVALID_STATE"

  Scenario: A member cannot use the librarian pages
    Given "alice" is logged in with password "Passw0rd!"
    When "alice" lists the users
    Then the response status is 403
    And the error is "ACCESS_DENIED"

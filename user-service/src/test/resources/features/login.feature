Feature: Logging in
  As a member
  I want to log in with my username and password
  So that I get a token to use the library

  Background:
    Given a verified member "alice" with password "Passw0rd!"

  Scenario: Right password
    When "alice" logs in with password "Passw0rd!"
    Then the response status is 200
    And the login gives a token for "alice" with role "member"

  Scenario: Wrong password
    When "alice" logs in with password "WrongPass1"
    Then the response status is 401
    And the error is "AUTHENTICATION_FAILED"
    And no token is given

  Scenario: Unknown username
    When "nobody" logs in with password "Passw0rd!"
    Then the response status is 401
    And the error is "AUTHENTICATION_FAILED"

  Scenario: The email address is not verified yet
    Given a member "bob" with password "Passw0rd!" who has not verified their email
    When "bob" logs in with password "Passw0rd!"
    Then the response status is 403
    And the error is "USER_NOT_VERIFIED"

  Scenario: The account was deactivated by a librarian
    Given the account "alice" is deactivated
    When "alice" logs in with password "Passw0rd!"
    Then the response status is 403
    And the error is "ACCOUNT_DEACTIVATED"

  Scenario: Username and password left empty
    When "" logs in with password ""
    Then the response status is 400
    And the error is "VALIDATION_FAILED"

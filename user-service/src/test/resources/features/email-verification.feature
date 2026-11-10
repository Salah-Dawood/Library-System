Feature: Email verification
  As a new member
  I want to confirm my email address with a code
  So that I can log in

  Background:
    Given a member "bob" with password "Passw0rd!" who has not verified their email

  Scenario: The member asks for a verification code
    When "bob" asks for a verification code
    Then the response status is 200
    And a 4 digit code is waiting for "bob" for 10 minutes

  Scenario: The right code verifies the account
    Given "bob" asked for a verification code
    When "bob" enters the code from the email
    Then the response status is 200
    And "bob" is verified
    And "bob" can log in with password "Passw0rd!"

  Scenario: A code can only be used once
    Given "bob" asked for a verification code
    And "bob" entered the code from the email
    When "bob" enters the same code again
    Then the response status is 400
    And the error is "INVALID_REQUEST"

  Scenario: A wrong code is refused
    Given "bob" asked for a verification code
    When "bob" enters a wrong code
    Then the response status is 400
    And the error is "INVALID_REQUEST"
    And "bob" is still not verified

  Scenario: An expired code is refused
    Given "bob" asked for a verification code
    And the code of "bob" has expired
    When "bob" enters the code from the email
    Then the response status is 400
    And the error is "INVALID_REQUEST"
    And "bob" is still not verified

  Scenario: Asking for a code for an unknown username
    When "nobody" asks for a verification code
    Then the response status is 404
    And the error is "RESOURCE_NOT_FOUND"

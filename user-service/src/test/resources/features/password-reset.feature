Feature: Password reset
  As a member who forgot my password
  I want a reset link by email
  So that I can choose a new password

  Background:
    Given a verified member "alice" with password "OldPassw0rd!"

  Scenario: A member asks for a reset link
    When someone asks for a password reset for "alice"
    Then the response status is 200
    And the answer is "If that account exists, a reset email has been sent."
    And a reset email was sent to "alice@example.com"

  Scenario: Asking for an unknown username gives the same answer and sends nothing
    When someone asks for a password reset for "nobody"
    Then the response status is 200
    And the answer is "If that account exists, a reset email has been sent."
    And no email was sent

  Scenario: The link from the email sets a new password
    Given "alice" asked for a password reset
    When the new password "NewPassw0rd!" is set with the link from the email
    Then the response status is 200
    And "alice" can log in with password "NewPassw0rd!"
    And "alice" cannot log in with password "OldPassw0rd!"

  Scenario: A made-up reset token is refused
    When the new password "NewPassw0rd!" is set with the token "not-a-real-token"
    Then the response status is 400
    And the error is "INVALID_REQUEST"

  Scenario: A login token cannot be used to reset a password
    Given "alice" can log in with password "OldPassw0rd!"
    When the new password "NewPassw0rd!" is set with the login token of "alice"
    Then the response status is 400
    And the error is "INVALID_REQUEST"

  Scenario: A reset token cannot be used to log in
    Given "alice" asked for a password reset
    When the reset token from the email is used to open the profile of "alice"
    Then the response status is 401

  Scenario: The new password is too short
    Given "alice" asked for a password reset
    When the new password "short" is set with the link from the email
    Then the response status is 400
    And the error is "VALIDATION_FAILED"

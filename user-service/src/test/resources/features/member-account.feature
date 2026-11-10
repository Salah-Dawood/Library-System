Feature: My account
  As a logged-in member
  I want to see and change my profile and password
  So that my account stays up to date and safe

  Background:
    Given a verified member "alice" with password "Passw0rd!"
    And "alice" is logged in with password "Passw0rd!"

  Scenario: The member sees their profile
    When "alice" opens their profile
    Then the response status is 200
    And the profile shows first name "Test", last name "User" and age 25

  Scenario: The member updates their profile
    When "alice" updates their profile to first name "Alice", last name "Smith", age 30 and bio "I love science fiction"
    Then the response status is 200
    And the profile of "alice" shows first name "Alice", last name "Smith" and age 30
    And the profile of "alice" shows bio "I love science fiction"

  Scenario: The member uploads a profile picture
    When "alice" uploads a real PNG picture
    Then the response status is 200
    And the profile picture of "alice" can be downloaded

  Scenario: A file that only pretends to be a picture is refused
    When "alice" uploads a text file named "picture.png"
    Then the response status is 400
    And the error is "INVALID_REQUEST"
    And "alice" has no profile picture

  Scenario: The first name is required when updating the profile
    When "alice" updates their profile to first name "", last name "Smith", age 30 and bio ""
    Then the response status is 400
    And the error is "VALIDATION_FAILED"

  Scenario: The member changes their password
    When "alice" changes their password from "Passw0rd!" to "NewPassw0rd!"
    Then the response status is 200
    And "alice" can log in with password "NewPassw0rd!"
    And "alice" cannot log in with password "Passw0rd!"

  Scenario: The current password must be right to change it
    When "alice" changes their password from "WrongPass1" to "NewPassw0rd!"
    Then the response status is 400
    And the error is "INVALID_REQUEST"
    And "alice" can log in with password "Passw0rd!"

  Scenario: The profile cannot be opened without logging in
    When someone without a token opens a profile
    Then the response status is 401

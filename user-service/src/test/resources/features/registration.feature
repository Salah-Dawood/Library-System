Feature: Registration
  As a visitor
  I want to create a BookNest account
  So that I can borrow books

  Scenario: A visitor registers with valid details
    When someone registers with:
      | username     | alice             |
      | emailAddress | alice@example.com |
      | password     | Passw0rd!         |
      | firstName    | Alice             |
      | lastName     | Smith             |
      | age          | 25                |
    Then the response status is 201
    And the new account "alice" is a "Member" that is not verified yet
    And the response does not show the password

  Scenario: The username is already taken
    Given an account "alice" with email "alice@example.com" already exists
    When someone registers as "alice" with email "other@example.com"
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"

  Scenario: The email address is already used
    Given an account "alice" with email "alice@example.com" already exists
    When someone registers as "bob" with email "alice@example.com"
    Then the response status is 409
    And the error is "RESOURCE_ALREADY_EXISTS"

  Scenario Outline: Invalid details are refused
    When someone registers with "<field>" set to "<value>"
    Then the response status is 400
    And the error is "VALIDATION_FAILED"
    And the field "<field>" is rejected

    Examples:
      | field        | value        |
      | username     | ab           |
      | emailAddress | not-an-email |
      | password     | short        |
      | firstName    |              |
      | age          | 12           |

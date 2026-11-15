Feature: Events published to Kafka
  As notification-service
  I want to hear about every new account
  So that I know where to send that member's emails, without calling user-service

  user-service publishes a UserRegistered event to the user-events topic; it does not know or wait for who reads it.

  Scenario: A registration publishes a UserRegistered event
    When someone registers as "alice" with email "alice@example.com"
    Then the response status is 201
    And a UserRegistered event for "alice" with the email "alice@example.com" is published

  Scenario: A refused registration publishes nothing
    Given an account "alice" with email "alice@example.com" already exists
    When someone registers as "alice" with email "other@example.com"
    Then the response status is 409
    And no UserRegistered event with the email "other@example.com" is published

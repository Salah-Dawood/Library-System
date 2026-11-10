Feature: Service health
  As someone running BookNest
  I want to ask user-service whether it is up
  So that Docker and AWS know when it can take traffic

  Scenario: The health endpoint reports the service as up
    When someone checks the health of the service
    Then the response status is 200
    And the service reports "UP"

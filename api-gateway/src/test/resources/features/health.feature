Feature: Gateway health
  As someone running BookNest
  I want to ask the gateway whether it is up
  So that Docker and AWS know when the site can take traffic

  Scenario: The health endpoint is answered by the gateway itself, not sent to the frontend
    When someone checks the health of the gateway
    Then the gateway reports "UP"

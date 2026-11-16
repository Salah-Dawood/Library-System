Feature: Gateway health
  As someone running BookNest
  I want to ask the gateway whether it is up
  So that Docker and AWS know when the site can take traffic

  Scenario: The health endpoint is answered by the gateway itself, not sent to the frontend
    When someone checks the health of the gateway
    Then the gateway reports "UP"

  Scenario: The gateway tells which release is running, for the version shown on the site
    When the browser asks for "/actuator/info"
    Then the gateway reports the version of this build

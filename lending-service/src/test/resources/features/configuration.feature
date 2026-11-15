Feature: Environment-specific configuration
  As a developer
  I want each environment (dev, test, prod) to pick up its own settings
  So that tests never touch a real database and production never shows test data

  These scenarios run with the test profile, the way the build and Jenkins run them.

  Scenario: The tests run on the test profile only
    Then the only active profile is "test"

  Scenario: The tests use an in-memory database, never PostgreSQL or RDS
    Then the database is an in-memory database

  Scenario: The sample data is only loaded outside the tests
    Then the sample data is not loaded

  Scenario: The race demo is only on where it is switched on (dev, and prod on AWS)
    Then the race demo endpoints are switched off

  Scenario Outline: The library rules come from the configuration
    Then the setting "<setting>" is "<value>"

    Examples:
      | setting                   | value |
      | app.fines.late-per-day    | 0.35  |
      | app.holds.hours           | 24    |
      | app.scheduler-lock.enabled | false |

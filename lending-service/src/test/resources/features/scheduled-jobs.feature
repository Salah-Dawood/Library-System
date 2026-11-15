Feature: Scheduled jobs with several lending-service instances
  As the library
  I want each scheduled job to run on only one lending-service instance at a time
  So that members never get the same reminder twice when two instances run behind the load balancer

  Before a job runs, the instance takes the job's row in the shedlock table; the other instances see it is
  taken and skip that run. The test profile turns the lock off for the other scenarios, so these scenarios
  use the lock with the settings declared on the jobs themselves.

  Scenario Outline: Each job is locked under its own name
    Then the "<job>" is locked under the name "<name>" for at least "<least>" and at most "<most>"

    Examples:
      | job              | name         | least | most |
      | due reminder job | dueReminders | 5m    | 10m  |
      | hold expiry job  | holdExpiry   | 30s   | 2m   |

  Scenario: Two instances start the due reminders at the same moment, only one sends them
    Given two lending-service instances share the database
    And both use the lock of the "due reminder job"
    When both instances start the job at the same moment
    Then the job ran 1 time
    And the shedlock table says the job is taken until at least 5 minutes from now

  Scenario: An instance whose clock is a little behind does not send the reminders again
    Given two lending-service instances share the database
    And both use the lock of the "due reminder job"
    When the first instance runs the job
    And the second instance starts the same job right after
    Then the job ran 1 time

  Scenario: Once the lock time is over, the next run happens
    Given two lending-service instances share the database
    And both use a lock that is kept for at least 1 second
    When the first instance runs the job
    And the second instance starts the same job 2 seconds later
    Then the job ran 2 times

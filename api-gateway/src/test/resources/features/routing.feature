Feature: Routing through the gateway
  As the browser
  I want one address for the whole of BookNest
  So that I never need to know which service owns which page or API

  Scenario Outline: Each path goes to the service that owns it
    When the browser asks for "<path>"
    Then "<service>" answers
    And it received the path "<path>"

    Examples:
      | path                          | service              |
      | /auth/users/login             | user-service         |
      | /auth/email/verify            | user-service         |
      | /api/member/profile           | user-service         |
      | /api/librarian/users          | user-service         |
      | /profile/avatar.png           | user-service         |
      | /api/books                    | catalog-service      |
      | /api/books/7                  | catalog-service      |
      | /api/authors                  | catalog-service      |
      | /api/genres                   | catalog-service      |
      | /api/reviews/3                | catalog-service      |
      | /api/loans/my-loans           | lending-service      |
      | /api/fines                    | lending-service      |
      | /api/waitlist                 | lending-service      |
      | /api/inventory                | lending-service      |
      | /api/audit-log                | lending-service      |
      | /api/demo/race/1              | lending-service      |
      | /api/notifications/stream     | notification-service |
      | /                             | frontend             |
      | /index.html                   | frontend             |
      | /css/app.css                  | frontend             |
      | /js/app.js                    | frontend             |
      | /js/pages/catalog.js          | frontend             |

  Scenario: An address nobody owns is not found
    When the browser asks for "/no-such-page"
    Then the response status is 404

  Scenario: The query string is passed on
    When the browser asks for "/api/books?title=dune&page=2"
    Then "catalog-service" answers
    And it received the query "title=dune&page=2"

  Scenario: The method, the login token and the body are passed on
    When the browser sends "POST" "/api/loans" with the token "abc.def.ghi" and the body "{\"bookId\":1,\"duration\":7}"
    Then "lending-service" answers
    And it received the method "POST", the token "abc.def.ghi" and the body "{\"bookId\":1,\"duration\":7}"

  Scenario: The service is told who the caller is, so per-user limits still work behind the gateway
    When the browser asks for "/auth/users/login"
    Then "user-service" answers
    And it was told the caller's address

  Scenario: A refusal from a service comes back unchanged
    When the browser asks for "/api/loans/conflict"
    Then the response status is 409
    And the response still says it was answered by "lending-service"

  Scenario: A service that is down gives an error instead of a hanging page
    Given "catalog-service" is down
    When the browser asks for "/api/books"
    Then the gateway answers with a server error

  Scenario: The other services keep working while one is down
    Given "catalog-service" is down
    When the browser asks for "/api/loans/my-loans"
    Then "lending-service" answers

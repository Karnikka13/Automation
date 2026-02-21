Feature: Session Registry Screen

  Background:
    Given User is already logged in
    And User navigates to Session Registry page

  Scenario: Execute Session Registry scenarios from Excel
    When User executes Session Registry scenarios from Excel sheet "Session_Registry"
    Then Session Registry results should be written to output sheet

Feature: Advanced Search Screen

  Background:
    Given User is already logged in
    And User navigates to the Advanced Search page

  Scenario: Execute Advanced Search scenarios from Excel
    When User executes Advanced Search scenarios from Excel sheet "advsearch"
    Then Advanced Search results should be written to output sheet

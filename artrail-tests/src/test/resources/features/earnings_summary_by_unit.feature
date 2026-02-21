Feature: Earnings Summary by Unit Screen

  Background:
    Given User is already logged in
    And User navigates to Earnings Summary by Unit page

  Scenario: Execute Earnings Summary by Unit scenarios from Excel
    When User executes Earnings Summary by Unit scenarios from Excel sheet "Earnings_By_Unit"
    Then Earnings Summary by Unit results should be written to output sheet

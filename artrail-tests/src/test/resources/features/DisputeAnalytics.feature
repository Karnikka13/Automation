Feature: Dispute Analytics Screen

  Background:
    Given User is already logged in
    And User navigates to Dispute Analytics page

  Scenario: Execute Dispute Analytics scenarios from Excel
    When User executes Dispute Analytics scenarios from Excel sheet "DisputeAnalytics_Input_final"
    Then Dispute Analytics results should be written to output sheet

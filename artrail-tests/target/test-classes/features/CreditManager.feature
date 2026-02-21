Feature: Credit Manager Screen

  Background:
    Given User is already logged in
    And User navigates to Credit Manager page

  Scenario: Execute Credit Manager scenarios from Excel
    When User executes Credit Manager scenarios from Excel sheet "CreditManager"
    Then Credit Manager results should be written to output sheet

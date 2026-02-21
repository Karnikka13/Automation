Feature: Payment and NSF Reversal Validation

  Background:
    Given User is already logged in
    And User navigates to Payment Reversal page

  Scenario: Execute Payment and NSF reversal scenarios from Excel
    When User executes Payment and NSF reversal scenarios from Excel sheet "PaymentReversal_Input1"
    Then Payment Reversal results should be written to output sheet

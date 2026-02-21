@ActiveSummary
Feature: Active Account Summary

Background:
  Given I am logged into the application using encrypted credentials from "src/test/resources/credentials.json"
  When I close the Active Account Summary tab if it is open
  And I navigate to the Active Account Summary section

Scenario: Activesum_001 - Default values on Active Account Summary page
  Then default values should be displayed on Active Account Summary page

Scenario: Activesum_002 - Client dropdown updates based on Corporate selection
 When I select a single corporate (after unselecting Select All)
 Then corresponding clients should be loaded in the Client dropdown

Scenario: Activesum_003 - Search results load when client is selected
 When I select a single client (after unselecting Select All)
 And I click the Search button
 Then Active Account Summary results should be displayed
  
Scenario: Activesum_004 - Date option shows last working date
  When I select Date option
  Then From and To date should be last working date
  And I click the Search button
  Then Active Account Summary results should be displayed
  
Scenario: Activesum_005 - Month option shows current month
 When I select Month option
  Then From and To month should be selected
  And I click the Search button
  Then Active Account Summary results should be displayed
  
Scenario: Activesum_006 - Year option shows current year
  When I select Year option
 Then From and To year should be current year
 And I click the Search button
 Then Active Account Summary results should be displayed
  
Scenario: Activesum_007 - Generate report with valid data
  When I enter valid report filters from excel
  And I click the Search button
  Then Active Account Summary results should be displayed

Scenario: Activesum_009 - Grid sorting based on user input
  When I apply grid sorting using values from excel
  Then Active Account Summary results should be displayed

Scenario: Activesum_010 - Show or Hide grid columns
  When I configure grid columns using values from excel
  Then Active Account Summary results should be displayed

Scenario: Activesum_011 - Export Active Account Summary to Excel
  When I export the Active Account Summary data to excel
  Then Export option should be available and export should be triggered

Scenario: Activesum_013 - Clear button resets all filters to default
  When I enter valid report filters from excel
  And I click the Search button
  Then Active Account Summary results should be displayed
  When I click the Clear button
  Then All fields should be cleared and default values should be loaded
  
Scenario: Activesum_014 - Brought Forward shown when account created before From Date
    When I select From Date and To Date from excel 
    And I click the Search button
    Then Active Account Summary results should be displayed
    And "BROUGHT_FORWARD" row should be displayed in summary
    When I click on "BROUGHT_FORWARD" row
    Then "Brought Forward" details grid should be displayed or empty

Scenario: Activesum_015 - New Listing shown for accounts created between From and To Date
    When I select From Date and To Date from excel
    And I click the Search button
    Then Active Account Summary results should be displayed
    And "NEW_LISTING" row should be displayed in summary
    When I click on "NEW_LISTING" row
    Then "New Listing" details grid should be displayed or empty

Scenario: Activesum_016 - Cancellation shown when cancelled date is between From and To Date
    When I select From Date and To Date from excel 
    And I click the Search button
    Then Active Account Summary results should be displayed
    And "CANCELLATION" row should be displayed in summary
    When I click on "CANCELLATION" row
    Then "Cancellation" details grid should be displayed or empty

Scenario: Activesum_017 - Paid In Full shown for cancelled accounts
    When I select From Date and To Date from excel
    And I click the Search button
    Then Active Account Summary results should be displayed
    And "PAID_IN_FULL" row should be displayed in summary
    When I click on "PAID_IN_FULL" row
    Then "Paid In Full" details grid should be displayed or empty

Scenario: Activesum_018 - Settled In Full shown for cancelled accounts
    When I select From Date and To Date from excel
    And I click the Search button
    Then Active Account Summary results should be displayed
    And "SETTLED_IN_FULL" row should be displayed in summary
    When I click on "SETTLED_IN_FULL" row
    Then "Settled In Full" details grid should be displayed or empty

Scenario: Activesum_019 - Payment shown for verified transactions
    When I select From Date and To Date from excel 
    And I click the Search button
    Then Active Account Summary results should be displayed
    And "PAYMENT" row should be displayed in summary
    When I click on "PAYMENT" row
    Then "Payment" details grid should be displayed or empty

Scenario: Activesum_020 - Payment Reversal shown for reversed transactions
    When I select From Date and To Date from excel 
    And I click the Search button
    Then Active Account Summary results should be displayed
    And "PAYMENT_REV" row should be displayed in summary
    When I click on "PAYMENT_REV" row
    Then "Payment Reversal" details grid should be displayed or empty

Scenario: Activesum_021 - Write Off accounts shown
    When I select From Date and To Date from excel 
    And I click the Search button
   Then Active Account Summary results should be displayed
    And "WRITE_OFF" row should be displayed in summary
   When I click on "WRITE_OFF" row
   Then "Write Off" details grid should be displayed or empty

Scenario: Activesum_022 - Carry Forward accounts shown
    When I select From Date and To Date from excel
    And I click the Search button
    Then Active Account Summary results should be displayed
    And "CARRY_FORWARD" row should be displayed in summary
    When I click on "CARRY_FORWARD" row
   Then "Carry Forward" details grid should be displayed or empty

Scenario: Activesum_023 - Select Execution Type as Real Time Data
  When I select Execution Type as Real Time Data

Scenario: Activesum_024 - Search summary grid using value from excel
  When I enter summary grid search value from excel
  Then Active Account Summary results should be displayed


    
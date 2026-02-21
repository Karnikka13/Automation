@CashReport
Feature: Cash Balancing Report

  Background:
    Given I am logged into the application using encrypted credentials from "src/test/resources/credentials.json"
    When I close the Cash Balancing Report tab if it is open
       
 Scenario: CashBal_001 - Verify Cash Balancing Report screen
 And I navigate to the Cash Balancing Report section
   Then Cash Balancing Report results should be displayed
 
 Scenario: CashBal_002 - Search Cash Balancing Report
 And I navigate to the Cash Balancing Report section
 When I enter Cash Balancing filters from excel
 And I click the Cash Balancing Search button
 Then Cash Balancing Report results should be displayed
 
 Scenario: CashBal_003 - Select Corporate 
 And I navigate to the Cash Balancing Report section
 When I enter Cash Balancing filters from excel
 When I select Corporate and wait for Client to auto load
 And I click the Cash Balancing Search button
 
Scenario: CashBal_004 - Open Client dropdown and observe default loading
And I navigate to the Cash Balancing Report section
When I click Client dropdown and observe default state

Scenario: CashBal_005 - Select Payment Type from Excel
And I navigate to the Cash Balancing Report section
When I enter Cash Balancing filters from excel
  When I select Payment Type from excel
  And I click the Cash Balancing Search button

Scenario: CashBal_006 - Select Transaction Type from Excel
And I navigate to the Cash Balancing Report section
When I enter Cash Balancing filters from excel
  When I select multiple Transaction Types from excel
 And I click the Cash Balancing Search button
 
Scenario: CashBal_007 - Select Group By from Excel
And I navigate to the Cash Balancing Report section
When I enter Cash Balancing filters from excel
  When I select Group By from excel
  And I click the Cash Balancing Search button
  
Scenario: CashBal_008 - Select Clear button
And I navigate to the Cash Balancing Report section
When I enter Cash Balancing filters from excel
When I click the Cash Balancing Clear button

Scenario: CashBal_009 - Export Cash Balancing Report to Excel 
And I navigate to the Cash Balancing Report section
   When I enter Cash Balancing filters from excel 
   And I click the Cash Balancing Search button
 When I export Cash Balancing Report as Excel and PDF

Scenario: ClientRec_001 - Verify Client Receivable screen
And I navigate to Client Receivable Paid section
Then Client Receivable Paid results should be displayed

Scenario: ClientRec_002 -  Search Client Receivable Paid
And I navigate to Client Receivable Paid section
When I enter Client Receivable Paid filters from excel
And I click Client Receivable Paid Search
Then Client Receivable Paid results should be displayed

Scenario: ClientRec_003 - Select Corporate in Client Receivable
And I navigate to Client Receivable Paid section
When I select Corporate and wait for Client to auto load in Client Receivable Paid
And I click Client Receivable Paid Search

Scenario: ClientRec_004 - Open Client dropdown and observe default loading
And I navigate to Client Receivable Paid section
When I click Client dropdown and observe default state for Client Receivable Paid

Scenario: ClientRec_005 - Select Transaction Type from Excel client receivable
And I navigate to Client Receivable Paid section
When I select multiple Transaction Types from excel for Client Receivable Paid
And I click Client Receivable Paid Search

Scenario: ClientRec_006 -Select Group By from Excel 
And I navigate to Client Receivable Paid section
When I enter Client Receivable Paid filters from excel
When I select Group By from excel for Client Receivable Paid
And I click Client Receivable Paid Search

Scenario: ClientRec_007 - Clear button click
And I navigate to Client Receivable Paid section
When I enter Client Receivable Paid filters from excel
And I click Client Receivable Paid Clear button

Scenario: ClientRec_008 -Export Cash Balancing Report to Excel 
And I navigate to Client Receivable Paid section
When I enter Client Receivable Paid filters from excel
And I click Client Receivable Paid Search
When I export Client Receivable Paid report as Excel and PDF



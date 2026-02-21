@AccountPurge
Feature: Account purge utility

Background:
  Given I am logged into the application using encrypted credentials from "src/test/resources/credentials.json"
  When I close the Account Purge Utility tab if it is open
  And I navigate to Account Purge Utility

Scenario: Accountpurge_001 - Account Purge Utility Page loaded 
 Then Account Purge Utility page should be loaded
  
Scenario: Accountpurge_002 - Click validate button
 When I click the Validate button in Account Purge Utility 
  
Scenario: Accountpurge_003 - 	Enter account number and validate  
 When I enter Account IDs for purge from Excel
 And I click the Validate button in Account Purge Utility

Scenario: Accountpurge_004 - Click validation in table 
  When I click Validate icon for the first validated batch
  And I confirm batch validation
 
Scenario: Accountpurge_005 - Click purge in table  
 When I click Purge for the validated batch
 And I confirm batch purge

Scenario: Accountpurge_006 - Empty rejection in table 
  When I enter Account IDs for purge from Excel
 And I click the Validate button in Account Purge Utility 
  When I click Reject for the batch
  And I confirm batch rejection

Scenario: Accountpurge_007 - Rejection with reason in table  
  When I click Reject for the batch
  And I confirm batch rejection

Scenario: Accountpurge_008 - Navigate to processing queue  
When I navigate to Processing queue tab

Scenario: Accountpurge_009 - Navigate to exception tab 
 When I navigate to Exception tab
  
Scenario: Accountpurge_010 - Click exception details 
  When I navigate to Exception tab
  When I open Exception details from table
  And I close the Exception details popup
 
Scenario: Accountpurge_012 - Rejection with reason in exception table
  When I navigate to Exception tab   
  When I click Reject for batch from Excel for exception
  And I submit reject reason for exception

Scenario: Accountpurge_013 - Enter date range for completed 
   When I navigate to Completed tab
   And I enter date range from Excel
   And I click submit for data range
   Then I verify whether grid displayed

Scenario: Accountpurge_014 - Click clear button for completed
    When I navigate to Completed tab
    And I enter date range from Excel
    And I clear the purge filters

Scenario: Accountpurge_015 - Enter date range for rejected
    When I navigate to Rejected tab
    When I apply rejected date range from Excel
And I submit rejected filter
Then I verify rejected grid is displayed

Scenario: Accountpurge_016 - Download from rejected tab
    When I navigate to Rejected tab
    When I apply rejected date range from Excel
    And I submit rejected filter
		Then I verify rejected grid is displayed
		When I click Download for rejected batch from Excel
    
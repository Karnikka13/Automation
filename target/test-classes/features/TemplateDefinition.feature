@TemplateDefinition
Feature: Template Definition Screen

Background:
  Given I am logged into the application using encrypted credentials from "src/test/resources/credentials.json"
 When I close the Check Template Definition tab if it is open
  And I navigate to the Check Template Definition screen
  
Scenario: TempDef_001 - Open Template Definition screen
 Then Check Template Definition results should be displayed

Scenario: TempDef_002 - Select record from grid
 When I double click first row in Check Template Definition grid
 Then Selected Check Definition row details should be loaded

Scenario: TempDef_003 - Valid add new Check Definition from Excel
 When I add new Check Definition from excel
 Then Check Template Definition results should be displayed

Scenario: TempDef_004 - Reset Check Definition form
 When I double click first row in Check Template Definition grid
 When I click Reset from dropdown in Check Template Definition screen
 Then Check Definition form should be cleared
  
Scenario: TempDef_005 - Check Definition History with filters
 When I click History from dropdown in Check Template Definition screen
 
Scenario: TempDef_006 - Click Edit from dropdown 
 When I click Edit from dropdown 

Scenario: TempDef_007 - Edit existing Check Definition from excel
 When I double click last row in Check Template Definition grid
 When I click Edit from dropdown
 And I update Check Definition details from excel

Scenario: TempDef_008 - Invalid add new Check Definition from Excel
  When I add new Check Definition from excel
  Then Check Template Definition results should be displayed
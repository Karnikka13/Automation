Feature: Login using Excel credentials

  Scenario: Login with multiple credentials from Excel
    Given Excel login page is opened
    When User logs in using credentials from Excel sheet "Login_Input"
    Then Login results should be written to Excel output

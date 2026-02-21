@ForwardAgency
Feature: Forward Agency Management

  Background:
    Given I am logged into the application using encrypted credentials from "src/test/resources/credentials.json"
    When I close the Forward Agency tab if it is open
    And I navigate to the Forward Agency section

  @edit_forward_agency
  Scenario: Edit forward agency records using Excel data
    When I edit all forward agency records from Excel file "src/test/resources/EditFile.xlsx"
    Then I should return to the grid view

  @search
  Scenario: Search forward agency records
  When I search for values from Excel in forward agency records
  Then I should see matching results in the grid
  
  
  @view_forward_agency_history
  Scenario: View history of a forward agency record
    When I view the history of the forward agency record


   @create_missing_phone
  Scenario: Create forward agency record with missing phone
    When I create a new forward agency record with test case "Missing Phone" from file "src/test/resources/new_data.json"
    Then I should return to the grid view


  @create_invalid_email
  Scenario: Create forward agency record with invalid email
    When I create a new forward agency record with test case "Invalid Email" from file "src/test/resources/new_data.json"
    Then I should return to the grid view


  @create_missing_email
  Scenario: Create forward agency record with missing email
    When I create a new forward agency record with test case "Missing Email" from file "src/test/resources/new_data.json"
    Then I should return to the grid view


  @create_missing_address
  Scenario: Create forward agency record with missing address
    When I create a new forward agency record with test case "Missing Address" from file "src/test/resources/new_data.json"
    Then I should return to the grid view


  @create_special_characters
  Scenario: Create forward agency record with special characters in name
    When I create a new forward agency record with test case "Special Characters Name" from file "src/test/resources/new_data.json"
    Then I should return to the grid view


  @create_long_values
  Scenario: Create forward agency record with long values
    When I create a new forward agency record with test case "Long Values" from file "src/test/resources/new_data.json"
    Then I should return to the grid view


  @create_valid_record
  Scenario: Create forward agency record with valid complete data
    When I create a new forward agency record with test case "Valid Complete Record" from file "src/test/resources/new_data.json"
    Then I should return to the grid view


  @create_inactive_agency
  Scenario: Create forward agency record as inactive agency
    When I create a new forward agency record with test case "Inactive Agency" from file "src/test/resources/new_data.json"
    Then I should return to the grid view


  
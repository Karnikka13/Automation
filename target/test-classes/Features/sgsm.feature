Feature: System Global Settings Master Navigation and Data Entry

  Background: User Login
    Given the user is logged into the System Global Settings Master application

  Scenario: Navigate to System Global Settings Master page, add first 2 entries, search and edit with 3rd record
    When I open the menu for System Global Settings
    And I search for System Global Settings Master in the menu search bar
    And I press Enter to navigate to System Global Settings
    And I wait for the System Global Settings Master page to load completely
    Then I should see the System Global Settings Master page
    And I load test data from "sgsmdata.json"
    And I add all settings entries from the JSON data
    Then all settings should be saved successfully
    When I load search data from "sgsmsearch.json"
    And I search and edit the entries from search data
    When I check the history to verify recent activity
    And I logout from System Global Settings
    Then the user should be logged out from System Global Settings successfully

Feature: Employment Demographic Navigation

  Scenario: Navigate to Employment Demographic, search, edit, fill, clear, refill, save, search and logout
    Given the user is logged into the application
    When I open the menu
    And I search for Employment Demographic in the menu search bar
    And I press Enter to navigate
    And I wait for the Employment Demographic page to load
    Then I should see the Employment Demographic page
    And I verify all fields are disabled
    When I search for employer from empp.json
    And I verify the details are auto-filled
    When I click on the ellipsis menu
    And I click on Edit option
    And I enter data in all Employment Demographic fields
    When I click on the ellipsis menu to clear
    And I click on Clear option
    And I click Yes on confirmation dialog
    When I click on the ellipsis menu
    And I click on Edit option
    And I enter data in all Employment Demographic fields
    When I click on the ellipsis menu to save
    And I click on Save option
    When I click on the ellipsis menu
    And I click on Edit option
    And I enter alternate data in all Employment Demographic fields
    When I click on the ellipsis menu to save
    And I click on Save option
    When I search for saved employment records
    When I logout
    Then the user should be logged out successfully
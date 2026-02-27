Feature: Check Printing Filter and Manual Check Creation

  Scenario: Login, navigate to Check Printing, apply filters, create manual checks, search and print, export data, and logout
    Given the user is logged into the Check Printing application
    When I open the menu for Check Printing
    And I search for Check Printing in the menu search bar
    And I press Enter to navigate to Check Printing
    And I wait for the Check Printing page to load completely
    Then I should see the Check Printing page

    When I enter first dataset and click cancel
    When I enter all datasets from JSON and apply filters
    When I create manual checks from JSON file
    
    When I search for checks and print them
    When I print individual check
    When I select all checks in each tab
    When I export data to Excel and PDF

    When I logout from Check Printing
    Then the user should be logged out from Check Printing successfully

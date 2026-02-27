Feature: Worklist Management
  As a user of the application
  I want to access and manage the worklist
  So that I can process work items efficiently

  Background:
    Given the user is logged into the application

  Scenario: Navigate to Worklist and start work queue with search
    When I navigate to Worklist
    And I wait for the Worklist page to load
    Then I should see the Worklist dashboard
    When I click on the Rebuild option
    And I click on Account Reg
    And I select the first available unit from the Worklist dropdown
    And I search for work items from the searchData.json file
    And I click on Start Work Queue
    And I wait for the queue page to fully load
    And I click on the Close button
    And I logout
    Then the user should be logged out successfully
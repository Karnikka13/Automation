Feature: County Management

  Scenario: Full County Workflow
    When I navigate to County Master
    And I search counties from JSON file "E:\\selenium\\search.json"
    And I insert counties from JSON file "E:\\selenium\\counties.json"
    And I update counties from JSON file "E:\\selenium\\updatecounty.json"
    And I clear counties from JSON file "E:\\selenium\\clear.json"
    And I view county history
    And I logout

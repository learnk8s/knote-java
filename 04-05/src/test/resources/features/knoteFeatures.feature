Feature: KNote Application

  Scenario: Display list of notes
    Given the application is running
    When I visit the home page
    Then I should see a list of notes

  Scenario: Save a note
    Given the application is running
    When I visit the home page
    And I enter a note description "description1"
    And I click the "Publish" button
    Then the note should be saved
    And I should see the note in the list of notes

  Scenario: Upload an image
    Given the application is running
    When I visit the home page
    And I choose an image file to upload "C:/Users/aksha/Downloads/what_is_image_Processing.avif"
    And I enter a note description "description2"
    And I click the "Upload" button 
    Then the image should be uploaded 
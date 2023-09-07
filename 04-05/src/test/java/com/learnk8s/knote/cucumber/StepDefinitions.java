package com.learnk8s.knote.cucumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import com.learnk8s.knote.Controller.KnoteController;
import com.learnk8s.knote.Note.Note;
import com.learnk8s.knote.Repository.NotesRepository;
import com.learnk8s.knote.UploadConfig.KnoteProperties;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class StepDefinitions {


    @Autowired
    private KnoteController controller;

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private KnoteProperties properties;

    private Model model;
    private  Note newNote = new Note();
    private ResponseEntity<List<Note>> GetResponse;
    private ResponseEntity<HttpStatusCode> SaveResponse;
    private List<Note> notes;
    private String Publish;
    private String Upload;
    private String description;
    private MultipartFile file;

    @Value("${server.port}")
    private int port;

    void save() throws Exception{
        SaveResponse=controller.saveNotes(file, description, Publish, Upload, model);
    }

    void upload(File imageFile) throws Exception{
        byte[] fileContent;
        fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile("file", imageFile.getName(), "image/avif", fileContent);
        file = multipartFile;
        save();
    }

    @Given("the application is running")
public void the_application_is_running() {
    // Write code here that turns the phrase above into concrete actions
    // throw new io.cucumber.java.PendingException();

    // to check if the application port is active, i.e. the application is running at this port
    assertDoesNotThrow(() -> new ServerSocket(port));
    notes=notesRepository.findAll();
}
@When("I visit the home page")
public void i_visit_the_home_page() {
    // Write code here that turns the phrase above into concrete actions
   // throw new io.cucumber.java.PendingException();

    model = new ExtendedModelMap();
   GetResponse = controller.index(model);
}
@Then("I should see a list of notes")
public void i_should_see_a_list_of_notes() {
    // Write code here that turns the phrase above into concrete actions
  //  throw new io.cucumber.java.PendingException();

  assertEquals(HttpStatus.OK,GetResponse.getStatusCode());
  assertEquals(notes.toString(),GetResponse.getBody().toString());
}

@When("I enter a note description {string}")
public void i_enter_a_note_description(String description) {
    // Write code here that turns the phrase above into concrete actions
   // throw new io.cucumber.java.PendingException();

   newNote.setDescription(description);
   this.description=description;
}
@When("I click the {string} button")
public void i_click_the_button(String button) {
    // Write code here that turns the phrase above into concrete actions
   // throw new io.cucumber.java.PendingException();

   switch (button){
    case "Publish":
       Publish="Publish";
       break;
    case "Upload":
       Upload="Upload";
       break;
    default:
       System.out.println("invalid operation input");
   }

   assertDoesNotThrow(()->save());
   Publish=null;
   Upload=null;
}
@Then("the note should be saved")
public void the_note_should_be_saved() {
    // Write code here that turns the phrase above into concrete actions
   // throw new io.cucumber.java.PendingException();

   assertEquals(HttpStatus.CREATED,SaveResponse.getBody());
   GetResponse = controller.index(model);
   assertEquals( HttpStatus.OK,GetResponse.getStatusCode());
   assertNotNull(GetResponse.getBody());
}
@Then("I should see the note in the list of notes")
public void i_should_see_the_note_in_the_list_of_notes() {
    // Write code here that turns the phrase above into concrete actions
   // throw new io.cucumber.java.PendingException();
   notes=notesRepository.findAll();
  assertEquals(notes.toString(),GetResponse.getBody().toString());
  assertTrue(GetResponse.getBody().toString().contains(newNote.toString()));
}

@When("I choose an image file to upload {string}")
public void i_choose_an_image_file_to_upload(String filepath) {
    // Write code here that turns the phrase above into concrete actions
    //throw new io.cucumber.java.PendingException();

    // Load the image file
    File imageFile = new File(filepath);
    assertDoesNotThrow(()->upload(imageFile));

    // Create a mock MultipartFile
    
}
@Then("the image should be uploaded")
public void the_image_should_be_uploaded() {
    // Write code here that turns the phrase above into concrete actions
    //throw new io.cucumber.java.PendingException();

    File uploadsDir = new File(properties.getUploadDir());
    assertTrue(uploadsDir.exists());
    assertEquals(HttpStatus.CREATED,SaveResponse.getBody());
}

}

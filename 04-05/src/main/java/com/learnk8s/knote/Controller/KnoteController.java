package com.learnk8s.knote.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.multipart.MultipartFile;

import com.learnk8s.knote.Note.Note;
import com.learnk8s.knote.Repository.NotesRepository;
import com.learnk8s.knote.UploadConfig.KnoteProperties;

import io.micrometer.core.ipc.http.HttpSender.Response;

import org.springframework.ui.Model;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

@Controller
public class KnoteController {

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private KnoteProperties properties;

    private Parser parser = Parser.builder().build();
    private HtmlRenderer renderer = HtmlRenderer.builder().build();


    @GetMapping("/")
    public ResponseEntity<List<Note>> index(Model model) {
		//System.out.println("+++++++++++++++++++++++++++"+model.toString());
        List<Note> notes=getAllNotes(model);
        return ResponseEntity.ok(notes);
		// return "index";
    }

    @PostMapping("/note")
    public ResponseEntity<HttpStatusCode> saveNotes(@RequestParam("image") MultipartFile file,
                            @RequestParam String description,
                            @RequestParam(required = false) String publish,
                            @RequestParam(required = false) String upload,
                            Model model) throws Exception {

		//System.out.println("ljnkjnwojbjwbcowb"+file.getOriginalFilename());

        // ResponseType response = new ResponseType();
        // String[] responseDescription = new String[2];

        if(upload==null&&publish==null){
            return ResponseEntity.ok(HttpStatus.BAD_REQUEST);
            // responseDescription[0]="Either Opt for \"Publish\" or \"Upload\"";
            // response.setResponseDescription(responseDescription);
            // response.setStatusCode(HttpStatus.BAD_REQUEST);
            // return response;
		}

        if (upload != null && upload.equals("Upload")) {
            if (file != null && file.getOriginalFilename() != null &&
                    !file.getOriginalFilename().isEmpty()) {
                uploadImage(file, description, model);
            }
            else{
                return ResponseEntity.ok(HttpStatus.BAD_REQUEST);
            //     response.setStatusCode(HttpStatus.BAD_REQUEST);
            //    responseDescription[0]="invalid file";
            //     response.setResponseDescription(responseDescription);
            //     return response;
            }
           // responseDescription[0]=("image: "+file.getOriginalFilename()+" is sucessfully uploaded");

           // return "index";
        }

        if (publish != null && publish.equals("Publish")) {
            saveNote(description, model);
            //responseDescription[1]=("note saved.....    note description: "+note.getDescription());

           // return "redirect:/";
        }

        // response.setStatusCode(HttpStatus.CREATED);
        // response.setResponseDescription(responseDescription);
        // response.setObject(getAllNotes(model));
        // response.setObject(getAllNotes(model));

		//return response;

        return ResponseEntity.ok(HttpStatus.CREATED);
    }


    private List<Note>  getAllNotes(Model model) {
        List<Note> notes = notesRepository.findAll();
        Collections.reverse(notes);
		model.addAttribute("notes", notes);
		return notes;
    }

    private void uploadImage(MultipartFile file, String description, Model model) throws Exception {

		File uploadsDir = new File(properties.getUploadDir());
        if (!uploadsDir.exists()) {
            uploadsDir.mkdir();
        }
        String fileId = UUID.randomUUID().toString() + "." +
                          file.getOriginalFilename().split("\\.")[1];
        file.transferTo(new File(uploadsDir.getAbsolutePath() + fileId));
		//System.out.println("image directory: ----------->   "+(uploadsDir.getAbsolutePath() + fileId).toString());
        model.addAttribute("description",
                description + " ![](/uploads/" + fileId + ")");
				
    }

    private void saveNote(String description, Model model) {
        if (description != null && !description.trim().isEmpty()) {
            //We need to translate markup to HTML
            org.commonmark.node.Node document = parser.parse(description.trim());
            String html = renderer.render(document);
            notesRepository.save(new Note(null, html));
            //After publish you need to clean up the textarea
            model.addAttribute("description","");
        }
    }

}


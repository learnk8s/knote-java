package io.learnk8s.knote;


import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
public class KnoteJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnoteJavaApplication.class, args);
    }

}

interface NotesRepository extends MongoRepository<Note, String> {

}

@Document(collection = "notes")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
class Note {
    @Id
    private String id;
    private String description;

    @Override
    public String toString() {
        return description;
    }
}

@Configuration
@EnableConfigurationProperties(KnoteProperties.class)
class KnoteConfig implements WebMvcConfigurer {

    @Autowired
    private KnoteProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + properties.getUploadDir())
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }

}

@ConfigurationProperties(prefix = "knote")
class KnoteProperties {
    @Value("${uploadDir:/tmp/uploads/}")
    private String uploadDir;

    @Value("${minio.host:localhost}")
    private String minioHost;

    @Value("${minio.bucket:image-storage}")
    private String minioBucket;

    @Value("${minio.port:9000}")
    private String minioPort;

    @Value("${minio.access.key:}")
    private String minioAccessKey;

    @Value("${minio.secret.key:}")
    private String minioSecretKey;

    @Value("${minio.useSSL:false}")
    private boolean minioUseSSL;

    public String getUploadDir() {
        return uploadDir;
    }

    public String getMinioHost() {
        return minioHost;
    }

    public String getMinioBucket() {
        return minioBucket;
    }

    public String getMinioPort() {
        return minioPort;
    }

    public String getMinioAccessKey() {
        return minioAccessKey;
    }

    public String getMinioSecretKey() {
        return minioSecretKey;
    }

    public boolean isMinioUseSSL() {
        return minioUseSSL;
    }
}

@Controller
class KNoteController {

    @Autowired
    private NotesRepository notesRepository;
    @Autowired
    private KnoteProperties properties;

    private Parser parser = Parser.builder().build();
    private HtmlRenderer renderer = HtmlRenderer.builder().build();

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        initMinio();
    }


    @GetMapping("/")
    public String index(Model model) {
        getAllNotes(model);
        return "index";
    }

    @PostMapping("/note")
    public String saveNotes(@RequestParam("image") MultipartFile file,
                            @RequestParam String description,
                            @RequestParam(required = false) String publish,
                            @RequestParam(required = false) String upload,
                            Model model) throws Exception {

        if (publish != null && publish.equals("Publish")) {
            saveNote(description, model);
        }
        if (upload != null && upload.equals("Upload")) {
            if (file != null && file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
                uploadImage(file, description, model);
            } else {
                // I want to keep the previous description
                model.addAttribute("description", description);
            }
        }
        getAllNotes(model);
        return "index";
    }

    @GetMapping(value = "/img/{name}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody
    byte[] getImageByName(@PathVariable String name) throws Exception {
        InputStream imageStream = minioClient.getObject(properties.getMinioBucket(), name);
        return IOUtils.toByteArray(imageStream);
    }

    private void getAllNotes(Model model) {
        List<Note> notes = notesRepository.findAll();
        Collections.reverse(notes);
        model.addAttribute("notes", notes);
    }

    private void uploadImage(MultipartFile file, String description, Model model) throws Exception {
        File uploadsDir = new File(properties.getUploadDir());
        if (!uploadsDir.exists()) {
            uploadsDir.mkdir();
        }
        String fileId = UUID.randomUUID().toString() + "." + file.getOriginalFilename().split("\\.")[1];
        //To store files locally
        //file.transferTo(new File(properties.getUploadDir() + fileId));
        //To use Minio Buckets
        minioClient.putObject(properties.getMinioBucket(), fileId, file.getInputStream(), file.getSize(), null, null, file.getContentType());
        model.addAttribute("description",
                description + " ![](/img/" + fileId + ")");
    }

    private void saveNote(String description, Model model) {
        if (description != null && !description.trim().isEmpty()) {
            //We need to translate markup to HTML
            Node document = parser.parse(description.trim());
            String html = renderer.render(document);
            notesRepository.save(new Note(null, html));
            //After publish we need to clean up the textarea
            model.addAttribute("description", "");
        }
    }

    private void initMinio() {
        try {
            // Create a minioClient with the MinIO Server name, Port, Access key and Secret key.
            String minioEndpoint = ((properties.isMinioUseSSL()) ? "https://" : "http://") + properties.getMinioHost() + ":" + properties.getMinioPort();
            minioClient = new MinioClient(minioEndpoint, properties.getMinioAccessKey(), properties.getMinioSecretKey());

            // Check if the bucket already exists.
            boolean isExist = minioClient.bucketExists(properties.getMinioBucket());
            if (isExist) {
                System.out.println("Bucket already exists.");
            } else {
                minioClient.makeBucket(properties.getMinioBucket());
            }

        } catch (Exception e) {
            System.out.println("Error occurred: " + e);
        }
    }

}

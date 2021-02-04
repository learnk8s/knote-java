**TL;DR:** In chapter 1 of this mini-series, you'll learn how to create a note-taking app, containerize it, and upload it to Docker Hub.

So you want to get started with Kubernetes, but don't understand where to begin.

In order to get comfortable with a new tool, you need hands-on experience to use it effectively.

That's why this mini-series was created.

In this series, you'll learn to develop an app using Spring Boot, deploy it to a local Kubernetes cluster, scale the app, and deploy it to Amazon Web Services on a production-grade Kubernetes cluster.

And in this particular article, you'll cover the development of an app that you'll use throughout this series.

Here's how the development will unfold.

## Table of contents

1. [Knote — a note taking app](#knote-a-note-taking-app)
1. MISSING
1. [Connecting a database](#connecting-a-database)
1. [Saving and retrieving notes](#saving-and-retrieving-notes)
1. [Rendering Markdown to HTML](#rendering-markdown-to-html)
1. [Uploading pictures](#uploading-pictures)
1. [Deploying apps with containers](#deploying-apps-with-containers)
1. [Linux containers](#linux-containers)
1. [Containerising the app](#containerising-the-app)
1. [Running the container](#running-the-container)
1. [Uploading the container image to a container registry](#uploading-the-container-image-to-a-container-registry)
1. [Recap and next steps](#recap-and-next-steps)

## Knote — a note taking app

The essential ingredient to learn how to deploy and scale applications in Kubernetes is the application itself.

As you'll learn throughout this course, **mastering Kubernetes doesn't guarantee that you have zero incidents in production.**

If your application isn't designed to be resilient and observable, the risk of downtime in production is high — even if you're using Kubernetes.

Learning how to design and architect applications that leverage Kubernetes is crucial.

And that's exactly what you will learn in this this part of the course.

Now you will develop a small application for note taking similar to [Evernote](https://evernote.com/) and [Google Keep](https://www.google.com/keep/).

The app should let you:

1. record notes and
1. attach images to your notes

Notes aren't lost when the app is killed or stopped.

So you will use a database to store the content.

Here is how the app looks like:

![Adding images and notes in Knote](assets/knote-add-image.gif)

The app is available [in this repository](https://github.com/learnk8s/knote-java/tree/master/01).

Go and check out the code with:

```terminal|command=1,2|title=bash
git clone https://github.com/learnk8s/knote-java
cd knote-java/01
```

You should launch and test the app locally.

You can install and start it with:

```terminal|command=1,2|title=bash
mvn clean install spring-boot:run
```

You can visit the app on <http://localhost:8080>.

TODO: missing MongoDB instructions?

Try to upload a picture — you should see a link inserted in the text box.

And when you publish the note, the picture should be displayed in the rendered note.

How is the app made?

## Boostrapping the app

First, you need to go to <https://start.spring.io> to generate the skeleton of the project:

![Setting up the project](assets/start.spring.io-project.jpg)

You should enter the **Group** and **Name** for your application:

- GroupId: learnk8s.io
- Name: knote-java

![Setting up the dependencies](assets/start.spring.io-dependencies.jpg)

Next, go to the dependencies section and choose:

- **Web** -> Spring Web Starter: basic web stack support in Spring Boot.
- **Actuator** -> Spring Boot Actuator: provide health endpoints for our application.
- **FreeMarker** -> Apache FreeMarker: templating engine for the HTML.
- **MongoDB** -> Spring Data MongoDB: driver and implementation for Spring Data interfaces to work with MongoDB.
- **Lombok** -> Lombok: library to avoid a lot of boilerplate code.

Then click _Generate the project_ to download a zip file containing the skeleton of your app.

Unzip the file and start a terminal session in that directory.

_You will do the front-end first._

Within `knote-java` application, there are two files in charge of rendering the Front End:

- [Tachyons CSS](https://github.com/learnk8s/knote-java/tree/master/01/src/main/resources/static) that needs to be placed inside `src/main/resources/static/`
- [Freemarker Template](https://github.com/learnk8s/knote-java/tree/master/01/src/main/resources/templates) for our index view that needs to be placed inside `src/main/resources/templates/`

> You can find the Freemarker template [in this repository](https://github.com/learnk8s/knote-java/tree/master/01).

Apache FreeMarker™ is a template engine: a Java library to generate text output (HTML web pages, e-mails, configuration files, source code, etc.) based on templates and changing data.

_With the front-end stuff out of the way, let's turn to code._

You can open our application in our favourite IDE and import it as a Maven Project.

Now, open `src/main/java/io/learnk8s/knote-java/KnoteJavaApplication.java`:

```java|title=KnoteJavaApplication.java
package io.learnk8s.knote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KnoteJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnoteJavaApplication.class, args);
    }

}
```

This is not much more than a standard Spring Boot Application.

It doesn't yet do anything useful.

_But you will change this now by connecting it to a database._

## Connecting a database

The database will store the notes.

_What database should you use? MySQL? Redis? Oracle?_

[MongoDB](https://www.mongodb.com/) is well-suited for your note-taking application because it's easy to set up and doesn't introduce the overhead of a relational database.

Because you had included the Spring Data MongoDB support, there is not much that you need to do to connect to the database.

You should open the `src/main/resources/application.properties` file and enter the URL for the database.

```properties|title=application.properties
spring.data.mongodb.uri=mongodb://localhost:27017/dev
```

**You have to consider something important here.**

_When the app starts, it shouldn't crash because the database isn't ready too._

Instead, the app should keep retrying to connect to the database until it succeeds.

Kubernetes expects that application components can be started in any order.

_If you make this code change, you can deploy your apps to Kubernetes in any order._

Luckily for you, Spring Data automatically reconnects to the database until the connection is successful.

_The next step is to use the database._

## Saving and retrieving notes

When the main page of your app loads, two things happen:

- All the existing notes are displayed
- Users can create new notes through an HTML form

_Let's address the displaying of existing notes first._

First, you should create a `Note` class that holds the note's details.

The same note is also stored in the "notes" MongoDB collection.

```java|title=KnoteJavaApplication
@SpringBootApplication
public class KnoteJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnoteJavaApplication.class, args);
    }
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
```

Next, you should leverage Spring Template to create a new repository to store the notes.

```java|highlight=17-19|title=KnoteJavaApplication.java
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

interface NotesRepository extends MongoRepository<Note, String> {

}
```

As you can see, you define an interface and Spring Data MongoDB generates the implementation.

Also, notice how the notes are persisted in the database:

- The type of the Note is Note
- The id of the notes is of type String

You should notice the two types in the interface signature `MongoRepository<Note, String>`.

You can access the repository by autowiring it.

You should create a new class with the `@Controller` annotation to select the views in your application.

```java|title=KnoteJavaApplication.java
...

@Controller
class KNoteController {

    @Autowired
    private NotesRepository notesRepository;

}
```

When a user accesses the `/` route, they should see all notes.

You should add a `@GetMapping` endpoint to return the FreeMarker template `index.ftl`.

> Please notice how we dropped the `.ftl` extension from the filename to refer to it.

```java|highlight=7-11|title=KnoteJavaApplication.java
@Controller
class KNoteController {

    @Autowired
    private NotesRepository notesRepository;

    @GetMapping("/")
    public String index(Model model) {
        getAllNotes(model);
        return "index";
    }

    private void getAllNotes(Model model) {
        List<Note> notes = notesRepository.findAll();
        Collections.reverse(notes);
        model.addAttribute("notes", notes);
    }
    ...
}
```

The `getAllNotes(Model model)` method is in charge of

1. retrieving all the notes stored in MongoDB
1. reversing the order of the notes (to show the last one first) and
1. updating the model consumed by the view

_Next, let's address the creation of new notes._

You should add a method to save a single note in the database:

```java|title=KnoteJavaApplication.java
private void saveNote(String description, Model model) {
  if (description != null && !description.trim().isEmpty()) {
    notesRepository.save(new Note(null, description.trim()));
    //After publish you need to clean up the textarea
    model.addAttribute("description", "");
  }
}
```

The form for creating notes is defined in the `index.ftl` template.

Note that the form handles both the creation of notes and the uploading of pictures.

The form submits to the `/note` route, so you need to another endpoint to your `@Controller`:

```java|title=KnoteJavaApplication.java
@PostMapping("/note")
public String saveNotes(@RequestParam("image") MultipartFile file,
                        @RequestParam String description,
                        @RequestParam(required = false) String publish,
                        @RequestParam(required = false) String upload,
                        Model model) throws IOException {

  if (publish != null && publish.equals("Publish")) {
    saveNote(description, model);
    getAllNotes(model);
    return "redirect:/";
  }
  // After save fetch all notes again
  return "index";
}
```

The above endpoint calls the `saveNote` method with the content of the text box, which causes the note to be saved in the database.

It then redirects to the main page ("index"), so that the newly created note appears immediately on the screen.

**Your app is functional now (although not yet complete)!**

You can already run your app at this stage.

But to do so, you need to run MongoDB as well.

You can install MongoDB following the instructions in the [official MongoDB documentation](https://docs.mongodb.com/manual/installation/).

Once MongoDB is installed, start a MongoDB server with:

```terminal|command=1|title=bash
mongod
```

Now run your app with:

```terminal|command=1|title=bash
mvn clean install spring-boot:run
```

The app should connect to MongoDB and then listen for requests.

You can access your app on <http://localhost:8080>.

You should see the main page of the app.

Try to create a note — you should see it being displayed on the main page.

_Your app seems to works._

**But it's not yet complete.**

The following requirements are missing:

- Markdown text is not formatted but just displayed verbatim
- Uploading pictures does not yet work

_Let's fix those next._

## Rendering Markdown to HTML

The Markdown notes should be rendered to HTML so that you can read them properly formatted.

You will use [commonmark-java](https://github.com/atlassian/commonmark-java) from Atlassian to parse the notes and render HTML.

But first you should add a dependency to your `pom.xml` file:

```xml|highlight=3-7|title=pom.xml
<dependencies>
  ...
  <dependency>
    <groupId>com.atlassian.commonmark</groupId>
    <artifactId>commonmark</artifactId>
    <version>0.12.1</version>
  </dependency>
  ...
</dependencies>
```

Then, change the `saveNote` method as follows (changed lines are highlighted):

```java|highlight=4-6|title=KnoteJavaApplication.java
private void saveNote(String description, Model model) {
  if (description != null && !description.trim().isEmpty()) {
    //You need to translate markup to HTML
    Node document = parser.parse(description.trim());
    String html = renderer.render(document);
    notesRepository.save(new Note(null, html));
    //After publish you need to clean up the textarea
    model.addAttribute("description", "");
  }
}
```

You also need to add to the `@Controller` itself:

```java|highlight=6-7|title=KnoteJavaApplication.java
@Controller
class KNoteController {

    @Autowired
    private NotesRepository notesRepository;
    private Parser parser = Parser.builder().build();
    private HtmlRenderer renderer = HtmlRenderer.builder().build();
```

The new code converts all the notes from Markdown to HTML before storing them into the database.

Kill the app with `CTRL + C` and then start it again:

```terminal|command=1|title=bash
mvn clean install spring-boot:run
```

Access it on <http://localhost:8080>.

Now you can add a note with the following text:

```markdown|title=snippet.md
Hello World! **Kubernetes Rocks!**
```

And you should see `Kubernetes Rocks!` in bold fonts.

**All your notes should now be nicely formatted.**

_Let's tackle uploading files._

## Uploading pictures

When a user uploads a picture, the file should be saved on disk, and a link should be inserted in the text box.

This is similar to how adding pictures on StackOverflow works.

> Note that for this to work, the picture upload endpoint must have access to the text box — this is the reason that picture uploading and note creation are combined in the same form.

For now, the pictures will be stored on the local file system.

Change the endpoint for the POST `/note` inside the `@Controller` (changed lines are highlighted):

```java|highlight=10-18,23-32|title=KnoteJavaApplication.java
@PostMapping("/note")
public String saveNotes(@RequestParam("image") MultipartFile file,
                        @RequestParam String description,
                        @RequestParam(required = false) String publish,
                        @RequestParam(required = false) String upload,
                        Model model) throws IOException {
  if (publish != null && publish.equals("Publish")) {
    saveNote(description, model);
    getAllNotes(model);
    return "redirect:/";
  }
  if (upload != null && upload.equals("Upload")) {
    if (file != null && file.getOriginalFilename() != null
          && !file.getOriginalFilename().isEmpty()) {
      uploadImage(file, description, model);
    }
    getAllNotes(model);
    return "index";
  }
  return "index";
}

private void uploadImage(MultipartFile file, String description, Model model) throws Exception {
  File uploadsDir = new File(properties.getUploadDir());
  if (!uploadsDir.exists()) {
    uploadsDir.mkdir();
  }
  String fileId = UUID.randomUUID().toString() + "."
                    + file.getOriginalFilename().split("\\.")[1];
  file.transferTo(new File(properties.getUploadDir() + fileId));
  model.addAttribute("description", description + " ![](/uploads/" + fileId + ")");
}
```

As you can see from the `uploadImage()` method, you are using Spring Boot configuration properties to inject application configurations.

These properties can be defined in the `application.properties` file or as environmental variables.

But you should define the `@ConfigurationProperties` class to retrieve those values.

Outside of the Controller class, you should define the `KnoteProperties` class annotated with `@ConfigurationProperties(prefix = "knote")`:

```java|title=KnoteJavaApplication.java
@ConfigurationProperties(prefix = "knote")
class KnoteProperties {
    @Value("${uploadDir:/tmp/uploads/}")
    private String uploadDir;

    public String getUploadDir() {
        return uploadDir;
    }
}
```

By default, the `uploadImage` method uses the `/tmp/uploads/` directory to store the images.

Notice that the `uploadImage` method checks if the directory exists and creates it if it doesn't.

> If you decide to change the path, make sure that the application has write access to that folder.

One last code change is required for the webserver (embedded in the spring boot application) to host files outside of the JVM classpath:

```java|title=KnoteJavaApplication.java
...
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
...
```

The class annotated with the `@Configuration` annotation maps the path `/uploads/` to the files located inside the `file:/tmp/uploads/` directory.

The class annotated with `@EnableConfigurationProperties(KnoteProperties.class)` allows Spring Boot to read and autowire the application properties.

You can override those properties in `application.properties` file or with environment variables.

Kill the app with `CTRL + C` and then start the application again:

```terminal|command=1|title=bash
mvn clean install spring-boot:run
```

Access it on <http://localhost:8080>.

Try to upload a picture — you should see a link is inserted in the text box.

And when you publish the note, the picture should be displayed in the rendered note.

**Your app is feature complete now.**

> Note that you can find the complete code for the app in [in this repository](https://github.com/learnk8s/knote-java/tree/01/).

You just created a simple note-taking app from scratch.

In the next section, you will learn how to package and run it as a Docker container.

You just created a simple note-taking app from scratch.

In the next section, you will learn how to package and run it as a Docker container.

## Deploying apps with containers

After creating your app, you can think about how to deploy it.

You could deploy our app to a Platform as a Service (PaaS) like [Heroku](https://www.heroku.com/) and forget about the underlying infrastructure and dependencies.

Or you could do it the hard way and provision your own VPS, [install nvm](https://github.com/nvm-sh/nvm), create the appropriate users, configure Node.js as well as [PM2](http://pm2.keymetrics.io/) to restart the app when it crashes and [Nginx](https://www.nginx.com/) to handle TLS and path-based routing.

However, in recent times, there is a trend to package applications as Linux containers and deploy them to specialised container platforms.

_So what are containers?_

## Linux containers

Linux containers are often compared to shipping containers.

Shipping containers have a standard format, and they allow to isolate goods in one container from goods in another.

Goods that belong together are packed in the same container, goods that have nothing to do with each other are packed in separate containers.

_Linux containers are similar._

**However, the "goods" in a Linux container are processes and their dependencies.**

> Typically, a container contains a single process and its dependencies.

A container contains everything that is needed to run a process.

That means that you can run a process without having to install any dependency on your machine because the container has all you need.

Furthermore, the process in a container is isolated from everything else around it.

When you run a container, the process isn't aware of the host machine, and it believes that it's the only process running on your computer.

Of course, that isn't true.

Bundling dependency and isolating processes might remind you of virtual machines.

**However, containers are different from virtual machines.**

The process in a container still executes on the kernel of the host machine.

With virtual machines, you run an entire guest operating system on top of your host operating system, and the processes that you want to execute on top of that.

Containers are much more lightweight than virtual machines.

**How do containers work?**

The magic of containers comes from two features in the Linux kernel:

- Control groups (cgroups)
- Namespaces

These are low-level Linux primitives.

Control groups limit the resources a process can use, such as memory and CPU.

_You might want to limit your container to only use up to 512 MB of memory and 10% of CPU time._

Namespaces limit what a process can see.

_You might want to hide the file system of your host machine and instead provide an alternative file system to the container._

You can imagine that those two features are convenient for isolating process.

However, they are very low-level and hard to work with.

Developers created more and more abstractions to get around the sharp edges of cgroups and namespaces.

These abstractions resulted in container systems.

One of the first container systems was [LXC](https://linuxcontainers.org/).

But the container breakthrough came with [Docker](https://www.docker.com/) which was released in 2013.

> Docker isn't the only Linux container technology. There are other popular projects such as [rkt](https://github.com/rkt/rkt) and [containerd](https://containerd.io/).

In this section, you will package your application as a Docker container.

_Let's get started!_

## Containerising the app

First of all, you have to install the Docker Community Edition (CE).

You can follow the instructions in the [official Docker documentation](https://docs.docker.com/install/).

> If you're on Windows, you can [follow our handy guide on how to install Docker on Windows](https://learnk8s.io/blog/installing-docker-and-kubernetes-on-windows/).

You can verify that Docker is installed correctly with the following command:

```terminal|command=1|title=bash
docker run hello-world

Hello from Docker!
This message shows that your installation appears to be working correctly.
```

**You're now ready to build Docker containers.**

Docker containers are built from Dockerfiles.

A Dockerfile is like a recipe — it defines what goes in a container.

A Dockerfile consists of a sequence of commands.

You can find the full list of commands in the [Dockerfile reference](https://docs.docker.com/engine/reference/builder/).

Here is a Dockerfile that packages your app into a container image:

```docker|title=Dockerfile
FROM adoptopenjdk/openjdk11:jdk-11.0.2.9-slim
WORKDIR /opt
ENV PORT 8080
EXPOSE 8080
COPY target/*.jar /opt/app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
```

Go on and save this as `Dockerfile` in the root directory of your app.

The above Dockerfile includes the following commands:

- [`FROM`](https://docs.docker.com/engine/reference/builder/#from) defines the base layer for the container, in this case, a version of OpenJDK 11
- ['WORKDIR'](https://docs.docker.com/engine/reference/builder/#workdir) sets the working directory to `/opt/`. Every subsequent instruction runs from within that folder
- ['ENV'](https://docs.docker.com/engine/reference/builder/#env) is used to set an environment variable
- [`COPY`](https://docs.docker.com/engine/reference/builder/#copy) copies the jar files from the `/target/` into the `/opt/` directory inside the container
- [`ENTRYPOINT`](https://docs.docker.com/engine/reference/builder/#entrypoint) executes `java $JAVA_OPTS -jar app.jar` inside the container

You can now build a container image from your app with the following command:

```terminal|command=1|title=bash
docker build -t knote-java .
```

Note the following about this command:

- `-t knote` defines the name ("tag") of your container — in this case, your container is just called `knote`
- `.` is the location of the Dockerfile and application code — in this case, it's the current directory

The command executes the steps outlined in the `Dockerfile`, one by one:

```animation
{
  "description": "Layers in Docker images",
  "animation": "assets/layers.svg",
  "fallback": "assets/layers-fallback.svg"
}
```

**The output is a Docker image.**

_What is a Docker image?_

A Docker image is an archive containing all the files that go in a container.

You can create many Docker containers from the same Docker image:

```animation
{
  "description": "Relationship between Dockerfiles, images and containers",
  "animation": "assets/dockerfile-image-container.svg",
  "fallback": "assets/docker-image-container-fallback.svg"
}
```

> Don't believe that Docker images are archives? Save the image locally with `docker save knote > knote.tar` and inspect it.

You can list all the images on your system with the following command:

```terminal|command=1|title=bash
docker images
REPOSITORY              TAG               IMAGE ID         CREATED            SIZE
knote-java              latest            dc2a8fd35e2e     30 seconds ago     165MB
adoptopenjdk/openjdk11  jdk-11.0.2.9-slim 9a223081d1a1     2 months ago       358MB
```

You should see the `knote` image that you built.

You should also see the `adoptopenjdk/openjdk11` which is the base layer of your `knote-java` image — it is just an ordinary image as well, and the `docker run` command downloaded it automatically from Docker Hub.

> Docker Hub is a container registry — a place to distribute and share container images.

_You packaged your app as a Docker image — let's run it as a container._

## Running the container

Remember that your app requires a MongoDB database.

In the previous section, you installed MongoDB on your machine and ran it with the `mongod` command.

You could do the same now.

_But guess what: you can run MongoDB as a container too._

MongoDB is provided as a Docker image named [`mongo`](https://hub.docker.com/_/mongo?tab=description) on Docker Hub.

_You can run MongoDB without actually "installing" it on your machine._

You can run it with `docker run mongo`.

**But before you do that, you need to connect the containers.**

The `knote` and `mongo` cointainers should communicate with each other, but they can do so only if they are on the same [Docker network](https://docs.docker.com/network/).

So, create a new Docker network as follows:

```terminal|command=1|title=bash
docker network create knote
```

**Now you can run MongoDB with:**

```terminal|command=1-4|title=bash
docker run \
  --name=mongo \
  --rm \
  --network=knote mongo
```

Note the following about this command:

- `--name` defines the name for the container — if you don't specify a name explicitly, then a name is generated automatically
- `--rm` automatically cleans up the container and removes the file system when the container exits
- `--network` represents the Docker network in which the container should run — when omitted, the container runs in the default network
- `mongo` is the name of the Docker image that you want to run

Note that the `docker run` command automatically downloads the `mongo` image from Docker Hub if it's not yet present on your machine.

MongoDB is now running.

**Now you can run your app as follows:**

```terminal|command=1-7|title=bash
docker run \
  --name=knote-java \
  --rm \
  --network=knote \
  -p 3000:3000 \
  -e MONGO_URL=mongodb://mongo:27017/dev \
  knote-java
```

Note the following about this command:

- `--name` defines the name for the container
- `--rm` automatically cleans up the container and removes the file system when the container exits
- `--network` represents the Docker network in which the container should run
- `-p 3000:3000` publishes port 3000 of the container to port 3000 of your local machine. That means, if you now access port 3000 on your computer, the request is forwarded to port 3000 of the Knote container. You can use the forwarding to access the app from your local machine.
- `-e` sets an environment variable inside the container

Regarding the last point, remember that your app reads the URL of the MongoDB server to connect to from the `MONGO_URL` environment variable.

If you look closely at the value of `MONGO_URL`, you see that the hostname is `mongo`.

_Why is it `mongo` and not an IP address?_

`mongo` is precisely the name that you gave to the MongoDB container with the `--name=mongo` flag.

If you named your MongoDB container `foo`, then you would need to change the value of `MONGO_URL` to `mongodb://foo:27017`.

**Containers in the same Docker network can talk to each other by their names.**

This is made possible by a built-in DNS mechanism.

_You should now have two containers running on your machine, `knote` and `mongo`._

You can display all running containers with the following command:

```terminal|command=1|title=bash
docker ps
CONTAINER ID    IMAGE       COMMAND                 PORTS                    NAMES
9b908ee0798a    knote-java  "/bin/sh -c 'exec ja…"  0.0.0.0:8080->8080/tcp   knote-java
1fb37b278231    mongo       "docker-entrypoint.s…"  27017/tcp                mongo
```

Great!

_It's time to test your application!_

Since you published port 3000 of your container to port 3000 of your local machine, your app is accessible on <http://localhost:3000>.

Go on and open the URL in your web browser.

**You should see your app!**

Verify that everything works as expected by creating some notes with pictures.

When you're done experimenting, stop and remove the containers as follows:

```terminal|command=1,2|title=bash
docker stop mongo knote-java
docker rm mongo knote-java
```

## Uploading the container image to a container registry

Imagine you want to share your app with a friend — how would you go about sharing your container image?

Sure, you could save the image to disk and send it to your friend.

_But there is a better way._

When you ran the MongoDB container, you specified its Docker Hub ID (`mongo`), and Docker automatically downloaded the image.

_You could create your images and upload them to DockerHub._

If your friend doesn't have the image locally, Docker automatically pulls the image from DockerHub.

> There exist other public container registries, such as [Quay](https://quay.io/) — however, Docker Hub is the default registry used by Docker.

**To use Docker Hub, you first have to [create a Docker ID](https://hub.docker.com/signup).**

A Docker ID is your Docker Hub username.

Once you have your Docker ID, you have to authorise Docker to connect to the Docker Hub account:

```terminal|command=1|title=bash
docker login
```

Before you can upload your image, there is one last thing to do.

**Images uploaded to Docker Hub must have a name of the form `username/image:tag`:**

- `username` is your Docker ID
- `image` is the name of the image
- `tag` is an optional additional attribute — often it is used to indicate the version of the image

To rename your image according to this format, run the following command:

```terminal|command=1|title=bash
docker tag knote <username>/knote-java:1.0.0
```

> Please replace `<username>` with your Docker ID.

**Now you can upload your image to Docker Hub:**

```terminal|command=1|title=bash
docker push <username>/knote-java:1.0.0
```

Your image is now publicly available as `<username>/knote-java:1.0.0` on Docker Hub and everybody can download and run it.

To verify this, you can re-run your app, but this time using the new image name.

> Please notice that the command below runs the `learnk8s/knote-java:1.0.0` image. If you wish to use yours, replace `learnk8s` with your Docker ID.

```terminal|command=1-5,6-12|title=bash
docker run \
  --name=mongo \
  --rm \
  --network=knote \
  mongo
docker run \
  --name=knote-java \
  --rm \
  --network=knote \
  -p 8080:8080 \
  -e MONGO_URL=mongodb://mongo:27017/dev \
  learnk8s/knote-java:1.0.0
```

Everything should work exactly as before.

**Note that now everybody in the world can run your application by executing the above two commands.**

And the app will run on their machine precisely as it runs on yours — without installing any dependencies.

_This is the power of containerisation!_

Once you're done testing your app, you can stop and remove the containers with:

```terminal|command=1,2|title=bash
docker stop mongo knote-java
docker rm mongo knote-java
```

Well done for making it this far!

## Recap and next steps

Here's a recap of what you've done so far.

1. You created an application using Express.js and MongoDB.
1. You packaged the app as a container using Docker.
1. You uploaded the container to Docker Hub — a container registry.
1. You ran the app and the databases locally using Docker.

[In the next section, you will learn how to run your containerised application on Kubernetes!]()

# knote-java

Simple Spring Boot app to take notes

You can find the following content in this repository branches:

- Branch `01`: simple application that you can run using mvn spring-boot:run, it requires mongoDB to be running. This application use the filesystem to store images
- Branch `02`: same application but now it contains a Dockerfile that you can use to package your application
- Branch `03`: same application but not inside the `kube/` directory you can find the kubernetes manifests to deploy the application into a kubernetes cluster
- Branch `04-05`: the application is now changed to use MinIO (Object Store) to upload images and being able to scale our application
- Branch `master`: you can find all the branches as folders

package com.learnk8s.knote.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.learnk8s.knote.Note.Note;

public interface NotesRepository extends MongoRepository<Note,String>{
    
}

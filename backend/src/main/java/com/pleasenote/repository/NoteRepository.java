package com.pleasenote.repository;

import com.pleasenote.model.Note;
import com.pleasenote.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByUser(User user);
}

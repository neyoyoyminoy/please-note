package com.pleasenote.repository;

import com.pleasenote.model.NoteRevision;
import com.pleasenote.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteRevisionRepository extends JpaRepository<NoteRevision, Long> {
    List<NoteRevision> findByNoteOrderByRevisionNumber(Note note);
}

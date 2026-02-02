package com.pleasenote.repository;

import com.pleasenote.model.Note;
import com.pleasenote.model.NoteRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NoteRevisionRepository extends JpaRepository<NoteRevision, Long> {

    List<NoteRevision> findByNoteOrderByRevisionNumber(Note note);

    @Query("SELECT nr FROM NoteRevision nr WHERE nr.note = :note ORDER BY nr.revisionNumber DESC LIMIT 1")
    Optional<NoteRevision> findLatestByNote(Note note);
}

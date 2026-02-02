package com.pleasenote.controller;

import com.pleasenote.model.Note;
import com.pleasenote.model.NoteRevision;
import com.pleasenote.model.User;
import com.pleasenote.repository.NoteRepository;
import com.pleasenote.repository.NoteRevisionRepository;
import com.pleasenote.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteRepository noteRepository;
    private final NoteRevisionRepository revisionRepository;
    private final UserRepository userRepository;

    public NoteController(NoteRepository noteRepository, NoteRevisionRepository revisionRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.revisionRepository = revisionRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @PostMapping
    public ResponseEntity<?> createNote(@RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        String title = body.get("title");
        String content = body.get("content");

        Note note = new Note();
        note.setUser(user);
        note.setTitle(title);
        note.setCreatedAt(OffsetDateTime.now());
        note.setUpdatedAt(OffsetDateTime.now());
        Note saved = noteRepository.save(note);

        NoteRevision revision = new NoteRevision();
        revision.setNote(saved);
        revision.setContent(content);
        revision.setRevisionNumber(1);
        revision.setCreatedAt(OffsetDateTime.now());
        NoteRevision savedRevision = revisionRepository.save(revision);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "noteId", saved.getId(),
            "revisionId", savedRevision.getId(),
            "revisionNumber", savedRevision.getRevisionNumber()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = getCurrentUser();
        Note note = noteRepository.findById(id)
            .orElse(null);

        if (note == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Note not found"));
        }

        Long lastRevisionId = ((Number) body.get("lastRevisionId")).longValue();
        String content = (String) body.get("content");
        String title = (String) body.getOrDefault("title", note.getTitle());

        NoteRevision latest = revisionRepository.findLatestByNote(note)
            .orElseThrow(() -> new RuntimeException("No revisions found"));

        if (!latest.getId().equals(lastRevisionId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Conflict: note was modified by another session",
                "currentRevisionId", latest.getId(),
                "currentRevisionNumber", latest.getRevisionNumber()
            ));
        }

        note.setTitle(title);
        note.setUpdatedAt(OffsetDateTime.now());
        noteRepository.save(note);

        NoteRevision revision = new NoteRevision();
        revision.setNote(note);
        revision.setContent(content);
        revision.setRevisionNumber(latest.getRevisionNumber() + 1);
        revision.setCreatedAt(OffsetDateTime.now());
        NoteRevision savedRevision = revisionRepository.save(revision);

        return ResponseEntity.ok(Map.of(
            "noteId", note.getId(),
            "revisionId", savedRevision.getId(),
            "revisionNumber", savedRevision.getRevisionNumber()
        ));
    }

    @GetMapping("/{id}/revisions")
    public ResponseEntity<?> getRevisions(@PathVariable Long id) {
        User user = getCurrentUser();
        Note note = noteRepository.findById(id)
            .orElse(null);

        if (note == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Note not found"));
        }

        if (!note.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not your note"));
        }

        List<NoteRevision> revisions = revisionRepository.findByNoteOrderByRevisionNumber(note);
        List<Map<String, Object>> result = revisions.stream().map(r -> Map.<String, Object>of(
            "revisionId", r.getId(),
            "revisionNumber", r.getRevisionNumber(),
            "content", r.getContent() != null ? r.getContent() : "",
            "createdAt", r.getCreatedAt()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}

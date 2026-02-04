package com.pleasenote.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "note_revisions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"note_id", "revision_number"})
})
public class NoteRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "note_id")
    private Note note;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Note getNote() { return note; }
    public void setNote(Note note) { this.note = note; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(int revisionNumber) { this.revisionNumber = revisionNumber; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

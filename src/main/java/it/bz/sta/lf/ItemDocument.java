package it.bz.sta.lf;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "item_documents")
public class ItemDocument {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "doc_type", nullable = false)
    private String docType;

    @Column(name = "doc_name")
    private String docName;

    @Column(name = "doc_birthdate")
    private LocalDate docBirthdate;

    @Column(name = "doc_issuer")
    private String docIssuer;

    @Column(name = "doc_number_full")
    private String docNumberFull;

    @Column(name = "doc_match_hash", nullable = false)
    private String docMatchHash;

    public ItemDocument() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public LocalDate getDocBirthdate() {
        return docBirthdate;
    }

    public void setDocBirthdate(LocalDate docBirthdate) {
        this.docBirthdate = docBirthdate;
    }

    public String getDocIssuer() {
        return docIssuer;
    }

    public void setDocIssuer(String docIssuer) {
        this.docIssuer = docIssuer;
    }

    public String getDocNumberFull() {
        return docNumberFull;
    }

    public void setDocNumberFull(String docNumberFull) {
        this.docNumberFull = docNumberFull;
    }

    public String getDocMatchHash() {
        return docMatchHash;
    }

    public void setDocMatchHash(String docMatchHash) {
        this.docMatchHash = docMatchHash;
    }
}

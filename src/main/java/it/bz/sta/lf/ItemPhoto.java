package it.bz.sta.lf;


import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity @Table(name = "item_photos")
public class ItemPhoto {
    @Id private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "item_id")
    private Item item;
    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;
    @Column(name = "content_type") private String contentType;
    @Column(name = "size_bytes") private Long sizeBytes;
    @Column(name = "uploaded_at") private OffsetDateTime uploadedAt = OffsetDateTime.now();


    public ItemPhoto() {}


    public UUID getId(){ return id; } public void setId(UUID id){ this.id=id; }
    public Item getItem(){ return item; } public void setItem(Item i){ this.item=i; }
    public String getObjectKey(){ return objectKey; } public void setObjectKey(String k){ this.objectKey=k; }
    public String getContentType(){ return contentType; } public void setContentType(String c){ this.contentType=c; }
    public Long getSizeBytes(){ return sizeBytes; } public void setSizeBytes(Long s){ this.sizeBytes=s; }
    public OffsetDateTime getUploadedAt(){ return uploadedAt; } public void setUploadedAt(OffsetDateTime t){ this.uploadedAt=t; }
}
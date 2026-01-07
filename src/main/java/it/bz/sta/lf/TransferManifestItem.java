package it.bz.sta.lf;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfer_manifest_items")
public class TransferManifestItem {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "manifest_id")
    private TransferManifest manifest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "short_code")
    private String shortCode;

    private String category; // we will treat this as categorySub

    @Column(name = "found_at")
    private OffsetDateTime foundAt;

    @Column(name = "found_place")
    private String foundPlace;

    @Column(name = "photo_key")
    private String photoKey;

    @Column(name = "category_main")
    private String categoryMain;

    public TransferManifestItem() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TransferManifest getManifest() { return manifest; }
    public void setManifest(TransferManifest manifest) { this.manifest = manifest; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public OffsetDateTime getFoundAt() { return foundAt; }
    public void setFoundAt(OffsetDateTime foundAt) { this.foundAt = foundAt; }

    public String getFoundPlace() { return foundPlace; }
    public void setFoundPlace(String foundPlace) { this.foundPlace = foundPlace; }

    public String getPhotoKey() { return photoKey; }
    public void setPhotoKey(String photoKey) { this.photoKey = photoKey; }

    public String getCategoryMain() { return categoryMain; }
    public void setCategoryMain(String categoryMain) { this.categoryMain = categoryMain; }

    public String getCategorySub() { return category; }
    public void setCategorySub(String categorySub) { this.category = categorySub; }
}

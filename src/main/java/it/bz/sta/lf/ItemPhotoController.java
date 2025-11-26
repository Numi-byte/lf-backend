package it.bz.sta.lf;


import it.bz.sta.lf.dto.ItemPhotoDto;
import it.bz.sta.lf.storage.S3StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;


import java.time.Duration;
import java.util.*;
import java.util.UUID;


@RestController
@RequestMapping("/items")
public class ItemPhotoController {
    private final ItemRepository items;
    private final ItemPhotoRepository photos;
    private final S3StorageService storage;


    public ItemPhotoController(ItemRepository items, ItemPhotoRepository photos, S3StorageService storage){
        this.items=items; this.photos=photos; this.storage=storage;
    }


    @PostMapping(path="/{id}/photos", consumes = {"multipart/form-data"})
    public ResponseEntity<ItemPhotoDto> upload(@PathVariable("id") UUID itemId, @RequestParam("file") MultipartFile file) throws Exception {
        Item item = items.findById(itemId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        String key = "items/" + itemId + "/" + UUID.randomUUID();
        try (var in = file.getInputStream()) {
            storage.put(key, in, file.getSize(), file.getContentType());
        }
        ItemPhoto p = new ItemPhoto();
        p.setId(UUID.randomUUID());
        p.setItem(item);
        p.setObjectKey(key);
        p.setContentType(file.getContentType());
        p.setSizeBytes(file.getSize());
        p = photos.save(p);
        String url = storage.presignGet(key, Duration.ofHours(1));
        return ResponseEntity.status(201).body(ItemPhotoDto.from(p, url));
    }


    @GetMapping("/{id}/photos")
    public List<ItemPhotoDto> list(@PathVariable("id") UUID itemId) throws Exception {
        items.findById(itemId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        List<ItemPhoto> list = photos.findByItemId(itemId);
        List<ItemPhotoDto> out = new ArrayList<>();
        for (ItemPhoto p : list) {
            String url = storage.presignGet(p.getObjectKey(), Duration.ofHours(1));
            out.add(ItemPhotoDto.from(p, url));
        }
        return out;
    }


    @DeleteMapping("/{itemId}/photos/{photoId}")
    public ResponseEntity<Void> delete(@PathVariable("itemId") UUID itemId, @PathVariable("photoId") UUID photoId) throws Exception {
        ItemPhoto p = photos.findById(photoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "photo not found"));
        if (!p.getItem().getId().equals(itemId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photo does not belong to item");
        storage.delete(p.getObjectKey());
        photos.delete(p);
        return ResponseEntity.noContent().build();
    }
}
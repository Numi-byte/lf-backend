package it.bz.sta.lf.dto;


import it.bz.sta.lf.ItemPhoto;
import java.time.OffsetDateTime;
import java.util.UUID;


public record ItemPhotoDto(UUID id, UUID itemId, String objectKey, String contentType, Long sizeBytes, OffsetDateTime uploadedAt, String url) {
    public static ItemPhotoDto from(ItemPhoto p, String url){
        return new ItemPhotoDto(p.getId(), p.getItem().getId(), p.getObjectKey(), p.getContentType(), p.getSizeBytes(), p.getUploadedAt(), url);
    }
}
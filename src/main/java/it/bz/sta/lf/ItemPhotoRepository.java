package it.bz.sta.lf;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.util.UUID;


public interface ItemPhotoRepository extends JpaRepository<ItemPhoto, UUID> {
    List<ItemPhoto> findByItemId(UUID itemId);
}
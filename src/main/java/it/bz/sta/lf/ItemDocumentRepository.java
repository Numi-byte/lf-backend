package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemDocumentRepository extends JpaRepository<ItemDocument, UUID> {

    List<ItemDocument> findByItem_Id(UUID itemId);

    List<ItemDocument> findByDocMatchHash(String docMatchHash);
}

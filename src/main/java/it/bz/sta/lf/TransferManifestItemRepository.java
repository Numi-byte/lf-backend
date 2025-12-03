package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferManifestItemRepository extends JpaRepository<TransferManifestItem, UUID> {
    List<TransferManifestItem> findByManifest_Id(UUID manifestId);
}

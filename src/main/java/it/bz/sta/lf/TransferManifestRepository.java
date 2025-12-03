package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferManifestRepository extends JpaRepository<TransferManifest, UUID> {

    List<TransferManifest> findByDepot_IdOrderByPreparedAtDesc(UUID depotId);

    List<TransferManifest> findByDepot_IdAndStatusOrderByPreparedAtDesc(UUID depotId, String status);
}

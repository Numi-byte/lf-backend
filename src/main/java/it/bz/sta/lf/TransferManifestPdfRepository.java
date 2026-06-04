package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferManifestPdfRepository extends JpaRepository<TransferManifestPdf, UUID> {
    Optional<TransferManifestPdf> findByManifest_IdAndLang(UUID manifestId, String lang);

    void deleteByManifest_Id(UUID manifestId);
}
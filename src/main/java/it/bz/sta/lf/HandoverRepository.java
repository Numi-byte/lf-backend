package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface HandoverRepository extends JpaRepository<Handover, UUID> {

    List<Handover> findByItemId(UUID itemId);

    @Query("""
        select h from Handover h
        where (:depotId is null or h.depotId = :depotId)
          and h.createdAt >= :fromTs
          and h.createdAt <= :toTs
          and (
               :text is null
            or :text = ''
            or lower(
                  concat(
                    coalesce(h.personName, ''), ' ',
                    coalesce(h.documentNumber, ''), ' ',
                    coalesce(h.comuneName, ''), ' ',
                    coalesce(h.notes, '')
                  )
               ) like lower(concat('%', :text, '%'))
          )
        order by h.createdAt desc
        """)
    List<Handover> search(
            @Param("depotId") UUID depotId,
            @Param("fromTs") OffsetDateTime fromTs,
            @Param("toTs") OffsetDateTime toTs,
            @Param("text") String text
    );
}

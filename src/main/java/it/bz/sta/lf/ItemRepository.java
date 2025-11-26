package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    // General search for UI, CS, depots
    @Query("""
        select i from Item i
        left join i.currentLocation l
        left join l.depot d
        where (:text is null or lower(i.description) like lower(concat('%', :text, '%')))
          and (:state is null or i.state = :state)
          and i.foundAt >= coalesce(:fromTs, i.foundAt)
          and i.foundAt <= coalesce(:toTs,   i.foundAt)
          and (:depotId is null or d.id = :depotId)
        order by i.foundAt desc
        """)
    List<Item> search(
            @Param("text") String text,
            @Param("state") String state,
            @Param("fromTs") OffsetDateTime from,
            @Param("toTs") OffsetDateTime to,
            @Param("depotId") UUID depotId
    );

    // Archive search for CS — only RETURNED and TRANSFERRED_TO_COMUNE
    @Query("""
        select i from Item i
        left join i.currentLocation l
        left join l.depot d
        where i.state in ('RETURNED','TRANSFERRED_TO_COMUNE')
          and (:text is null or lower(i.description) like lower(concat('%', :text, '%')))
          and i.foundAt >= coalesce(:fromTs, i.foundAt)
          and i.foundAt <= coalesce(:toTs,   i.foundAt)
          and (:depotId is null or d.id = :depotId)
        order by i.foundAt desc
        """)
    List<Item> searchArchive(
            @Param("text") String text,
            @Param("fromTs") OffsetDateTime from,
            @Param("toTs") OffsetDateTime to,
            @Param("depotId") UUID depotId
    );
}

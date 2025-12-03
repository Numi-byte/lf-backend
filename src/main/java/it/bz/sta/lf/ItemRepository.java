package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    // General search: filter by state + depot in DB
    @Query("""
        select i from Item i
        left join i.currentLocation l
        left join l.depot d
        where (:state is null or i.state = :state)
          and (:depotId is null or d.id = :depotId)
        order by i.foundAt desc
        """)
    List<Item> search(
            @Param("state") String state,
            @Param("depotId") UUID depotId
    );

    // Archive search: only RETURNED + TRANSFERRED_TO_COMUNE, plus depot in DB
    @Query("""
        select i from Item i
        left join i.currentLocation l
        left join l.depot d
        where i.state in ('RETURNED','TRANSFERRED_TO_COMUNE')
          and (:depotId is null or d.id = :depotId)
        order by i.foundAt desc
        """)
    List<Item> searchArchive(
            @Param("depotId") UUID depotId
    );

    // Used by READY_FOR_TRANSFER and Comune manifest
    List<Item> findByCurrentLocation_Depot_IdAndState(UUID depotId, String state);

    // Used to find items older than X days in a depot
    List<Item> findByCurrentLocation_Depot_IdAndStateAndFoundAtBefore(
            UUID depotId,
            String state,
            OffsetDateTime foundAt
    );
}

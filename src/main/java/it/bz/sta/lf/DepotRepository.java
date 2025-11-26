package it.bz.sta.lf;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface DepotRepository extends JpaRepository<Depot, UUID> {}
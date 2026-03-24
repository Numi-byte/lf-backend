package it.bz.sta.lf;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "items")
public class Item {

    public static final String STATE_REPORTED = "REPORTED";
    public static final String STATE_SHELVED = "SHELVED";
    public static final String STATE_ON_HOLD = "ON_HOLD";
    public static final String STATE_READY_FOR_TRANSFER = "READY_FOR_TRANSFER";
    public static final String STATE_RETURNED = "RETURNED";
    public static final String STATE_TRANSFERRED_TO_COMUNE = "TRANSFERRED_TO_COMUNE";

    @Id
    private UUID id;

    private String description;

    @Column(name = "found_at")
    private OffsetDateTime foundAt;

    @Column(nullable = false)
    private String state = STATE_REPORTED;

    @Column(name = "category_main", nullable = false)
    private String categoryMain = "MISC";

    @Column(name = "category_sub", nullable = false)
    private String categorySub = "OTHER";

    @Column(name = "transport_type")
    private String transportType;

    @Column(name = "transport_line")
    private String transportLine;

    @Column(name = "transport_line_de")
    private String transportLineDe;

    @Column(nullable = false)
    private String company = CompanyAccessService.DEFAULT_COMPANY;

    @ManyToOne
    @JoinColumn(name = "current_location_id")
    private Location currentLocation;

    public Item() {}

    public Item(UUID id, String description, OffsetDateTime foundAt) {
        this.id = id;
        this.description = description;
        this.foundAt = foundAt;
        this.state = STATE_REPORTED;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getFoundAt() { return foundAt; }
    public void setFoundAt(OffsetDateTime foundAt) { this.foundAt = foundAt; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }


    public String getCategoryMain() { return categoryMain; }
    public void setCategoryMain(String categoryMain) { this.categoryMain = categoryMain; }

    public String getCategorySub() { return categorySub; }
    public void setCategorySub(String categorySub) { this.categorySub = categorySub; }

    public String getTransportType() { return transportType; }
    public void setTransportType(String transportType) { this.transportType = transportType; }

    public String getTransportLine() { return transportLine; }
    public void setTransportLine(String transportLine) { this.transportLine = transportLine; }

    public String getTransportLineDe() { return transportLineDe; }
    public void setTransportLineDe(String transportLineDe) { this.transportLineDe = transportLineDe; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public Location getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Location currentLocation) { this.currentLocation = currentLocation; }
}

package it.bz.sta.lf;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;


@Entity
@Table(name = "depots")
public class Depot {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String company = CompanyAccessService.DEFAULT_COMPANY;


    public Depot() {}
    public Depot(UUID id, String name, String company) {
        this.id = id;
        this.name = name;
        this.company = company;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }


    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
}
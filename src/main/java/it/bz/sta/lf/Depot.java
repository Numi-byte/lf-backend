package it.bz.sta.lf;


import jakarta.persistence.*;
import java.util.UUID;


@Entity @Table(name = "depots")
public class Depot {
    @Id private UUID id;
    @Column(nullable = false) private String name;


    public Depot() {}
    public Depot(UUID id, String name){ this.id=id; this.name=name; }


    public UUID getId(){ return id; }
    public void setId(UUID id){ this.id=id; }
    public String getName(){ return name; }
    public void setName(String n){ this.name=n; }
}
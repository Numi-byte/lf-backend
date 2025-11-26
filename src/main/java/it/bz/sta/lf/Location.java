package it.bz.sta.lf;


import jakarta.persistence.*;
import java.util.UUID;


@Entity @Table(name = "locations")
public class Location {
    @Id private UUID id;


    @ManyToOne(optional = false) @JoinColumn(name = "depot_id")
    private Depot depot;


    @Column(nullable = false) private String zone;
    @Column(name = "row_no", nullable = false) private Integer rowNo;
    @Column(name = "col_no", nullable = false) private Integer colNo;
    private String bin;
    @Column(nullable = false) private String type; // shelf | locker


    public Location() {}


    public UUID getId(){ return id; } public void setId(UUID id){ this.id=id; }
    public Depot getDepot(){ return depot; } public void setDepot(Depot d){ this.depot=d; }
    public String getZone(){ return zone; } public void setZone(String z){ this.zone=z; }
    public Integer getRowNo(){ return rowNo; } public void setRowNo(Integer v){ this.rowNo=v; }
    public Integer getColNo(){ return colNo; } public void setColNo(Integer v){ this.colNo=v; }
    public String getBin(){ return bin; } public void setBin(String b){ this.bin=b; }
    public String getType(){ return type; } public void setType(String t){ this.type=t; }
}
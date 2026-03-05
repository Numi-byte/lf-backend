package it.bz.sta.lf.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "catalog_visibility_rules")
public class CatalogVisibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "main_code", nullable = false)
    private String mainCode;

    @Column(name = "sub_code")
    private String subCode;

    public Long getId() {
        return id;
    }

    public String getMainCode() {
        return mainCode;
    }

    public void setMainCode(String mainCode) {
        this.mainCode = mainCode;
    }

    public String getSubCode() {
        return subCode;
    }

    public void setSubCode(String subCode) {
        this.subCode = subCode;
    }
}

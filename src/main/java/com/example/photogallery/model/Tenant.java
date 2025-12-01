package com.example.photogallery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")

public class Tenant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();


    public Tenant() {}
    public Tenant(String slug, String name){
        this.slug = slug;
        this.name = name;
    }
    
    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug;}
    public String getName() {return name;}
    public void setName(String name) { this.name = name;}
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt; }
        
    

}

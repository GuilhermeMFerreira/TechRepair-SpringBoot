package com.dt2.techrepair.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Tecnicos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String nome;
    private String especialidades;
    private String email;

    public Tecnicos() {}

    public Tecnicos(String nome, String especialidades, String email) {
        this.nome = nome;
        this.especialidades = especialidades;
        this.email = email;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEspecialidades() { return especialidades; }
    public void setEspecialidades(String especialidades) { this.especialidades = especialidades; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

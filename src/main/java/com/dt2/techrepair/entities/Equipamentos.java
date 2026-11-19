package com.dt2.techrepair.entities;

import jakarta.persistence.*;

@Entity
public class Equipamentos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String tipo;
    private String marca;
    private String modelo;
    private String numero_serie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Clientes cliente;

    public Equipamentos() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getNumero_serie() { return numero_serie; }
    public void setNumero_serie(String numero_serie) { this.numero_serie = numero_serie; }
    public Clientes getCliente() { return cliente; }
    public void setCliente(Clientes cliente) { this.cliente = cliente; }
}

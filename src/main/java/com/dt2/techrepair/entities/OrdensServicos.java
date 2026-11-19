package com.dt2.techrepair.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class OrdensServicos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String descricaoProblema;
    private String diagnostico;

    @Enumerated(EnumType.STRING)
    private StatusOS status = StatusOS.ABERTA;

    private double valor;
    private LocalDate dataAbertura;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipamento_id")
    private Equipamentos equipamento;

    @ManyToOne
    @JoinColumn(name = "tecnico_id")
    private Tecnicos tecnico;

    public OrdensServicos() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDescricaoProblema() { return descricaoProblema; }
    public void setDescricaoProblema(String descricaoProblema) { this.descricaoProblema = descricaoProblema; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public StatusOS getStatus() { return status; }
    public void setStatus(StatusOS status) { this.status = status; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public LocalDate getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDate dataAbertura) { this.dataAbertura = dataAbertura; }
    public Equipamentos getEquipamento() { return equipamento; }
    public void setEquipamento(Equipamentos equipamento) { this.equipamento = equipamento; }
    public Tecnicos getTecnico() { return tecnico; }
    public void setTecnico(Tecnicos tecnico) { this.tecnico = tecnico; }
}

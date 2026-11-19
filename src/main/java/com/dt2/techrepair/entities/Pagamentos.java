package com.dt2.techrepair.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Pagamentos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private double valor;
    private String metodoPagamento;

    @Enumerated(EnumType.STRING)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    private String referencia;
    private LocalDateTime confirmadoEm;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ordem_id")
    private OrdensServicos ordem;

    public Pagamentos() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public StatusPagamento getStatus() { return status; }
    public void setStatus(StatusPagamento status) { this.status = status; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public void setConfirmadoEm(LocalDateTime confirmadoEm) { this.confirmadoEm = confirmadoEm; }
    public OrdensServicos getOrdem() { return ordem; }
    public void setOrdem(OrdensServicos ordem) { this.ordem = ordem; }
}

package com.dt2.techrepair.controller;

import com.dt2.techrepair.entities.*;
import com.dt2.techrepair.repositories.OrdensServicosRepository;
import com.dt2.techrepair.repositories.PagamentosRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/pagamentos")
public class PagamentosController {

    record Confirmar(String referencia) {}

    private final PagamentosRepository repository;
    private final OrdensServicosRepository ordens;

    public PagamentosController(PagamentosRepository repository, OrdensServicosRepository ordens) {
        this.repository = repository;
        this.ordens = ordens;
    }

    // Soma o que já foi registrado na ordem (pendente + confirmado), sem contar estornos.
    private double comprometido(int ordemId, int ignorarId) {
        return repository.findByOrdemId(ordemId).stream()
                .filter(p -> p.getStatus() != StatusPagamento.ESTORNADO && p.getId() != ignorarId)
                .mapToDouble(Pagamentos::getValor).sum();
    }

    private void validar(Pagamentos p, OrdensServicos o, int ignorarId) {
        if (p.getValor() <= 0) throw Http.bad("O valor deve ser maior que zero.");
        if (p.getMetodoPagamento() == null || p.getMetodoPagamento().isBlank()) throw Http.bad("Escolha a forma de pagamento.");
        if (o.getValor() <= 0) throw Http.bad("A ordem ainda não tem valor de orçamento.");
        if (comprometido(o.getId(), ignorarId) + p.getValor() > o.getValor() + 0.005)
            throw Http.bad("Os pagamentos passariam do valor da ordem (R$ " + String.format("%.2f", o.getValor()) + ").");
    }

    @PostMapping
    public Pagamentos criarPagamentos(@RequestBody Pagamentos pagamentos) {
        if (pagamentos.getOrdem() == null) throw Http.bad("Escolha a ordem de serviço.");
        OrdensServicos o = ordens.findById(pagamentos.getOrdem().getId()).orElseThrow(() -> Http.bad("Ordem não encontrada."));
        if (o.getStatus() == StatusOS.CANCELADA) throw Http.bad("Ordem cancelada não aceita pagamento.");
        validar(pagamentos, o, 0);
        pagamentos.setId(0);
        pagamentos.setOrdem(o);
        // O status nunca vem da tela: todo pagamento nasce pendente.
        pagamentos.setStatus(StatusPagamento.PENDENTE);
        pagamentos.setReferencia(null);
        pagamentos.setConfirmadoEm(null);
        return repository.save(pagamentos);
    }

    @GetMapping
    public List<Pagamentos> mostrar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pagamentos> buscarId(@PathVariable int id) {
        return ResponseEntity.of(repository.findById(id));
    }

    @PutMapping("/{id}")
    public Pagamentos atualizar(@PathVariable int id, @RequestBody Pagamentos p) {
        Pagamentos pagamentos = repository.findById(id).orElseThrow(Http::notFound);
        if (pagamentos.getStatus() != StatusPagamento.PENDENTE)
            throw Http.conflict("Só pagamento pendente pode ser editado.");
        validar(p, pagamentos.getOrdem(), id);
        pagamentos.setValor(p.getValor());
        pagamentos.setMetodoPagamento(p.getMetodoPagamento());
        return repository.save(pagamentos);
    }

    @PutMapping("/{id}/confirmar")
    public Pagamentos confirmar(@PathVariable int id, @RequestBody Confirmar body) {
        Pagamentos p = repository.findById(id).orElseThrow(Http::notFound);
        if (p.getStatus() != StatusPagamento.PENDENTE) throw Http.conflict("Este pagamento não está pendente.");
        if (body.referencia() == null || body.referencia().isBlank())
            throw Http.bad("Informe o comprovante (ID da transação Pix/cartão ou número do recibo).");
        p.setStatus(StatusPagamento.CONFIRMADO);
        p.setReferencia(body.referencia().trim());
        p.setConfirmadoEm(LocalDateTime.now());
        return repository.save(p);
    }

    @PutMapping("/{id}/estornar")
    public Pagamentos estornar(@PathVariable int id) {
        Pagamentos p = repository.findById(id).orElseThrow(Http::notFound);
        if (p.getStatus() != StatusPagamento.CONFIRMADO) throw Http.conflict("Só pagamento confirmado pode ser estornado.");
        p.setStatus(StatusPagamento.ESTORNADO);
        return repository.save(p);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable int id) {
        Pagamentos p = repository.findById(id).orElseThrow(Http::notFound);
        if (p.getStatus() != StatusPagamento.PENDENTE)
            throw Http.conflict("Pagamento confirmado ou estornado fica no histórico e não pode ser excluído.");
        repository.deleteById(id);
    }
}

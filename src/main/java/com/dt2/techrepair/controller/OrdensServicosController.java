package com.dt2.techrepair.controller;

import com.dt2.techrepair.entities.*;
import com.dt2.techrepair.repositories.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dt2.techrepair.entities.StatusOS.*;

@RestController
@RequestMapping("api/ordensservicos")
public class OrdensServicosController {

    // Fluxo permitido: o servidor é quem decide, a tela só mostra as opções.
    private static final Map<StatusOS, Set<StatusOS>> PROXIMOS = Map.of(
            ABERTA, Set.of(EM_DIAGNOSTICO, CANCELADA),
            EM_DIAGNOSTICO, Set.of(AGUARDANDO_APROVACAO, CANCELADA),
            AGUARDANDO_APROVACAO, Set.of(EM_REPARO, CANCELADA),
            EM_REPARO, Set.of(CONCLUIDA, CANCELADA),
            CONCLUIDA, Set.of(ENTREGUE),
            ENTREGUE, Set.of(),
            CANCELADA, Set.of());

    record MudarStatus(StatusOS status) {}

    private final OrdensServicosRepository repository;
    private final EquipamentosRepository equipamentos;
    private final TecnicosRepository tecnicos;
    private final PagamentosRepository pagamentos;

    public OrdensServicosController(OrdensServicosRepository repository, EquipamentosRepository equipamentos,
                                    TecnicosRepository tecnicos, PagamentosRepository pagamentos) {
        this.repository = repository;
        this.equipamentos = equipamentos;
        this.tecnicos = tecnicos;
        this.pagamentos = pagamentos;
    }

    private Tecnicos tecnico(OrdensServicos o) {
        if (o.getTecnico() == null || o.getTecnico().getId() == 0) return null;
        return tecnicos.findById(o.getTecnico().getId()).orElseThrow(() -> Http.bad("Técnico não encontrado."));
    }

    private double pago(int ordemId) {
        return pagamentos.findByOrdemId(ordemId).stream()
                .filter(p -> p.getStatus() == StatusPagamento.CONFIRMADO)
                .mapToDouble(Pagamentos::getValor).sum();
    }

    @PostMapping
    public OrdensServicos criarOrdensServicos(@RequestBody OrdensServicos ordensservicos) {
        if (ordensservicos.getEquipamento() == null) throw Http.bad("Escolha o equipamento.");
        if (ordensservicos.getDescricaoProblema() == null || ordensservicos.getDescricaoProblema().isBlank())
            throw Http.bad("Descreva o problema relatado.");
        ordensservicos.setId(0);
        ordensservicos.setEquipamento(equipamentos.findById(ordensservicos.getEquipamento().getId())
                .orElseThrow(() -> Http.bad("Equipamento não encontrado.")));
        ordensservicos.setTecnico(tecnico(ordensservicos));
        ordensservicos.setStatus(ABERTA);
        ordensservicos.setValor(0);
        ordensservicos.setDataAbertura(LocalDate.now());
        return repository.save(ordensservicos);
    }

    @GetMapping
    public List<OrdensServicos> mostrar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdensServicos> buscarId(@PathVariable int id) {
        return ResponseEntity.of(repository.findById(id));
    }

    @PutMapping("/{id}")
    public OrdensServicos atualizar(@PathVariable int id, @RequestBody OrdensServicos o) {
        OrdensServicos os = repository.findById(id).orElseThrow(Http::notFound);
        if (os.getStatus() == ENTREGUE || os.getStatus() == CANCELADA)
            throw Http.conflict("Ordem encerrada não pode ser editada.");
        if (o.getValor() < 0) throw Http.bad("O valor não pode ser negativo.");
        os.setDescricaoProblema(o.getDescricaoProblema());
        os.setDiagnostico(o.getDiagnostico());
        os.setValor(o.getValor());
        os.setTecnico(tecnico(o));
        return repository.save(os);
    }

    @PutMapping("/{id}/status")
    public OrdensServicos mudarStatus(@PathVariable int id, @RequestBody MudarStatus body) {
        OrdensServicos os = repository.findById(id).orElseThrow(Http::notFound);
        StatusOS novo = body.status();
        if (novo == null || !PROXIMOS.get(os.getStatus()).contains(novo))
            throw Http.conflict("Não é possível ir de " + os.getStatus() + " para " + novo + ".");
        if (novo == AGUARDANDO_APROVACAO && os.getValor() <= 0)
            throw Http.bad("Informe o valor do orçamento antes de pedir a aprovação do cliente.");
        if (novo == EM_REPARO && os.getTecnico() == null)
            throw Http.bad("Escolha o técnico responsável antes de iniciar o reparo.");
        if (novo == ENTREGUE && pago(id) + 0.005 < os.getValor())
            throw Http.conflict("Entrega bloqueada: pagamento não confirmado (pago R$ "
                    + String.format("%.2f", pago(id)) + " de R$ " + String.format("%.2f", os.getValor()) + ").");
        os.setStatus(novo);
        return repository.save(os);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable int id) {
        repository.deleteById(id);
    }
}

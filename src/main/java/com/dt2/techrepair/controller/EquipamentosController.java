package com.dt2.techrepair.controller;

import com.dt2.techrepair.entities.Clientes;
import com.dt2.techrepair.entities.Equipamentos;
import com.dt2.techrepair.repositories.ClientesRepository;
import com.dt2.techrepair.repositories.EquipamentosRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/equipamentos")
public class EquipamentosController {

    private final EquipamentosRepository repository;
    private final ClientesRepository clientes;

    public EquipamentosController(EquipamentosRepository repository, ClientesRepository clientes) {
        this.repository = repository;
        this.clientes = clientes;
    }

    private Clientes dono(Equipamentos e) {
        if (e.getCliente() == null) throw Http.bad("Escolha o cliente dono do equipamento.");
        return clientes.findById(e.getCliente().getId()).orElseThrow(() -> Http.bad("Cliente não encontrado."));
    }

    @PostMapping
    public Equipamentos criarEquipamentos(@RequestBody Equipamentos equipamentos) {
        equipamentos.setId(0);
        equipamentos.setCliente(dono(equipamentos));
        return repository.save(equipamentos);
    }

    @GetMapping
    public List<Equipamentos> mostrar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipamentos> buscarId(@PathVariable int id) {
        return ResponseEntity.of(repository.findById(id));
    }

    @PutMapping("/{id}")
    public Equipamentos atualizar(@PathVariable int id, @RequestBody Equipamentos e) {
        Equipamentos equipamentos = repository.findById(id).orElseThrow(Http::notFound);
        equipamentos.setTipo(e.getTipo());
        equipamentos.setMarca(e.getMarca());
        equipamentos.setModelo(e.getModelo());
        equipamentos.setNumero_serie(e.getNumero_serie());
        equipamentos.setCliente(dono(e));
        return repository.save(equipamentos);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable int id) {
        repository.deleteById(id);
    }
}

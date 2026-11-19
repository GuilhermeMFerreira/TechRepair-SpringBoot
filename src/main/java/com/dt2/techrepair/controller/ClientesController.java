package com.dt2.techrepair.controller;

import com.dt2.techrepair.entities.Clientes;
import com.dt2.techrepair.repositories.ClientesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/clientes")
public class ClientesController {

    private final ClientesRepository repository;
    public ClientesController(ClientesRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public void criarClientes(@RequestBody Clientes clientes) {
        repository.save(clientes);
    }

    @GetMapping
    public List<Clientes> mostrar() {
        return repository.findAll();
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable int id) {

        repository.deleteById(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Clientes> buscarId(@PathVariable int id) {
        return ResponseEntity.of(repository.findById(id));
    }

    @PutMapping("/{id}")
    public void atualizar (@PathVariable int id, @RequestBody Clientes c){
        Clientes clientes = repository.getReferenceById(id);
        clientes.setNome(c.getNome());
        clientes.setEmail(c.getEmail());
        clientes.setTelefone(c.getTelefone());
        repository.save(clientes);
    }
}

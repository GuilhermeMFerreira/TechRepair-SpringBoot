package com.dt2.techrepair.controller;

import com.dt2.techrepair.entities.Tecnicos;
import com.dt2.techrepair.repositories.TecnicosRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/tecnicos")
public class TecnicosController {

    private final TecnicosRepository repository;
    public TecnicosController(TecnicosRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public void criarTecnicos(@RequestBody Tecnicos tecnicos) {
        repository.save(tecnicos);
    }

    @GetMapping
    public List<Tecnicos> mostrar() {
        return repository.findAll();
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable int id) {

        repository.deleteById(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tecnicos> buscarId(@PathVariable int id) {
        return ResponseEntity.of(repository.findById(id));
    }

    @PutMapping("/{id}")
    public void atualizar (@PathVariable int id, @RequestBody Tecnicos t){
        Tecnicos tecnicos = repository.getReferenceById(id);
        tecnicos.setNome(t.getNome());
        tecnicos.setEspecialidades(t.getEspecialidades());
        tecnicos.setEmail(t.getEmail());
        repository.save(tecnicos);
    }
}

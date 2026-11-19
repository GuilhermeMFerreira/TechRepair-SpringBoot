package com.dt2.techrepair.repositories;

import com.dt2.techrepair.entities.Pagamentos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentosRepository extends JpaRepository<Pagamentos, Integer> {
    List<Pagamentos> findByOrdemId(int ordemId);
}

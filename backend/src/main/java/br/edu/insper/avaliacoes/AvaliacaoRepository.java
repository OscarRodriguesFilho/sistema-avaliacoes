package br.edu.insper.avaliacoes;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findAllByOrderByDataAvaliacaoDescIdDesc();
}

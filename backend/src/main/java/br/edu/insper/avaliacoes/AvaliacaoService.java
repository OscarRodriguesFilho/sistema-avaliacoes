package br.edu.insper.avaliacoes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AvaliacaoService {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final AvaliacaoRepository repository;

    public AvaliacaoService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    public Avaliacao cadastrar(AvaliacaoRequest request, String emailDoToken) {
        String avaliador = temTexto(emailDoToken) ? emailDoToken : request.emailAvaliador();
        if (!temTexto(avaliador)) {
            throw new AvaliacaoInvalidaException("Informe o email do avaliador");
        }
        avaliador = normalizar(avaliador);
        String avaliado = normalizar(request.emailAvaliado());
        if (avaliador.equals(avaliado)) {
            throw new AvaliacaoInvalidaException("O avaliador não pode avaliar a si mesmo");
        }
        LocalDate data = request.dataAvaliacao() != null ? request.dataAvaliacao() : LocalDate.now(FUSO);
        return repository.save(new Avaliacao(avaliador, avaliado, request.comentarios().trim(), request.nota(), data));
    }

    public List<Avaliacao> listar() {
        return repository.findAllByOrderByDataAvaliacaoDescIdDesc();
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

package br.edu.insper.avaliacoes;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoController {
    private final AvaliacaoService service;
    private final String emailClaim;

    public AvaliacaoController(AvaliacaoService service, @Value("${app.auth0.email-claim}") String emailClaim) {
        this.service = service;
        this.emailClaim = emailClaim;
    }

    /** Somente ADMIN (regra em SecurityConfig). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Avaliacao cadastrar(@Valid @RequestBody AvaliacaoRequest request, @AuthenticationPrincipal Jwt jwt) {
        String emailDoToken = jwt != null ? jwt.getClaimAsString(emailClaim) : null;
        return service.cadastrar(request, emailDoToken);
    }

    /** ADMIN e USER (regra em SecurityConfig). */
    @GetMapping
    public List<Avaliacao> listar() {
        return service.listar();
    }
}

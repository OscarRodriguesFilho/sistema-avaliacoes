package br.edu.insper.avaliacoes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Corpo do POST /avaliacoes.
 * emailAvaliador é opcional: quando o token traz o email do usuário logado, ele tem prioridade.
 * dataAvaliacao é opcional: quando ausente, a API usa a data de hoje.
 */
public record AvaliacaoRequest(
        @Email(message = "deve ser um email válido") String emailAvaliador,
        @NotBlank(message = "é obrigatório") @Email(message = "deve ser um email válido") String emailAvaliado,
        @NotBlank(message = "é obrigatório") @Size(max = 2000, message = "deve ter no máximo 2000 caracteres") String comentarios,
        @NotNull(message = "é obrigatória") @Min(value = 0, message = "deve estar entre 0 e 10") @Max(value = 10, message = "deve estar entre 0 e 10") Integer nota,
        @PastOrPresent(message = "não pode estar no futuro") LocalDate dataAvaliacao) {
}

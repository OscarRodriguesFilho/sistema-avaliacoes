package br.edu.insper.avaliacoes;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AvaliacaoControllerTest {
    private static final String NS = "https://avaliacoes-api";
    private static final Auth0RolesConverter ROLES = new Auth0RolesConverter(NS + "/roles");

    private static final String CORPO_VALIDO = """
            {
              "emailAvaliado": "maria@empresa.com",
              "comentarios": "Ótima colaboração no projeto",
              "nota": 9,
              "dataAvaliacao": "2026-11-10"
            }
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private AvaliacaoRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    /** Simula um access token do Auth0 com as claims que a Action adiciona. */
    private static JwtRequestPostProcessor token(String email, String... roles) {
        return jwt()
                .jwt(j -> j.subject("auth0|" + email).claim(NS + "/roles", List.of(roles)).claim(NS + "/email", email))
                .authorities(ROLES);
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/avaliacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/avaliacoes").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userNaoPodeCadastrar() throws Exception {
        mockMvc.perform(post("/avaliacoes").with(token("joao@empresa.com", "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenSemRoleNaoPodeListar() throws Exception {
        mockMvc.perform(get("/avaliacoes").with(token("visitante@empresa.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCadastraEUserLista() throws Exception {
        mockMvc.perform(post("/avaliacoes").with(token("admin@empresa.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.emailAvaliador").value("admin@empresa.com"))
                .andExpect(jsonPath("$.emailAvaliado").value("maria@empresa.com"))
                .andExpect(jsonPath("$.nota").value(9))
                .andExpect(jsonPath("$.dataAvaliacao").value("2026-11-10"));

        mockMvc.perform(get("/avaliacoes").with(token("joao@empresa.com", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].comentarios").value("Ótima colaboração no projeto"));
    }

    @Test
    void emailDoTokenTemPrioridadeSobreOCorpo() throws Exception {
        mockMvc.perform(post("/avaliacoes").with(token("admin@empresa.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailAvaliador": "outra@empresa.com", "emailAvaliado": "maria@empresa.com",
                                 "comentarios": "Bom trabalho", "nota": 7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailAvaliador").value("admin@empresa.com"))
                .andExpect(jsonPath("$.dataAvaliacao").isNotEmpty());
    }

    @Test
    void notaForaDoIntervaloRetorna400() throws Exception {
        mockMvc.perform(post("/avaliacoes").with(token("admin@empresa.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailAvaliado": "maria@empresa.com", "comentarios": "Teste", "nota": 11}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nota").value("deve estar entre 0 e 10"));
    }

    @Test
    void naoPodeAvaliarASiMesmo() throws Exception {
        mockMvc.perform(post("/avaliacoes").with(token("maria@empresa.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("O avaliador não pode avaliar a si mesmo"));
    }
}

package br.edu.insper.avaliacoes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String emailAvaliador;

    @Column(nullable = false)
    private String emailAvaliado;

    @Column(nullable = false, length = 2000)
    private String comentarios;

    @Column(nullable = false)
    private Integer nota;

    @Column(nullable = false)
    private LocalDate dataAvaliacao;

    protected Avaliacao() { }

    public Avaliacao(String emailAvaliador, String emailAvaliado, String comentarios, Integer nota, LocalDate dataAvaliacao) {
        this.emailAvaliador = emailAvaliador;
        this.emailAvaliado = emailAvaliado;
        this.comentarios = comentarios;
        this.nota = nota;
        this.dataAvaliacao = dataAvaliacao;
    }

    public Long getId() { return id; }
    public String getEmailAvaliador() { return emailAvaliador; }
    public String getEmailAvaliado() { return emailAvaliado; }
    public String getComentarios() { return comentarios; }
    public Integer getNota() { return nota; }
    public LocalDate getDataAvaliacao() { return dataAvaliacao; }
}

package br.com.sistemas.chamados.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelUsuario papel;

    @Column(nullable = false)
    private boolean ativo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", unique = true)
    private Cliente cliente;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id", unique = true)
    private Tecnico tecnico;

    protected Usuario() { }

    public Usuario(
        String email,
        String senhaHash,
        PapelUsuario papel,
        boolean ativo,
        Cliente cliente,
        Tecnico tecnico
    ) {
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.ativo = ativo;
        this.cliente = cliente;
        this.tecnico = tecnico;
    }

    @AssertTrue(message = "Os vínculos da identidade devem corresponder ao papel")
    public boolean isVinculosConsistentes() {
        if (papel == null) {
            return false;
        }

        return switch (papel) {
            case CLIENTE -> cliente != null && tecnico == null;
            case TECNICO -> tecnico != null && cliente == null;
            case ADMINISTRADOR -> cliente == null && tecnico == null;
        };
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }

    @JsonIgnore
    public String getSenhaHash() { return senhaHash; }

    public PapelUsuario getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }
    public Cliente getCliente() { return cliente; }
    public Tecnico getTecnico() { return tecnico; }
}

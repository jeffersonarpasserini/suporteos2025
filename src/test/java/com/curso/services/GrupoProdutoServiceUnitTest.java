package com.curso.services;

import com.curso.domains.GrupoProduto;
import com.curso.domains.Produto;
import com.curso.domains.enums.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrupoProdutoServiceUnitTest {

    @Test
    void defaultConstructorShouldStartWithActiveStatusAndEmptyList() {
        GrupoProduto grupo = new GrupoProduto();

        assertEquals(Status.ATIVO, grupo.getStatus());
        assertTrue(grupo.getProdutos().isEmpty());
    }

    @Test
    void addProdutoShouldAttachBothSides() {
        GrupoProduto grupo = new GrupoProduto();
        Produto produto = new Produto();
        produto.setCodigoBarra("1234567890123");
        produto.setDescricao("Produto Teste");

        grupo.addProduto(produto);

        assertTrue(grupo.getProdutos().contains(produto));
        assertSame(grupo, produto.getGrupoProduto());
    }

    @Test
    void removeProdutoShouldDetachWhenSameGroup() {
        GrupoProduto grupo = new GrupoProduto();
        Produto produto = new Produto();
        produto.setCodigoBarra("1234567890123");
        produto.setDescricao("Produto Teste");
        grupo.addProduto(produto);

        grupo.removeProduto(produto);

        assertTrue(grupo.getProdutos().isEmpty());
        assertNull(produto.getGrupoProduto());
    }

    @Test
    void equalsAndHashCodeShouldUseIdAndDescricao() {
        GrupoProduto grupo1 = new GrupoProduto();
        grupo1.setId(1);
        grupo1.setDescricao("Grupo");

        GrupoProduto grupo2 = new GrupoProduto();
        grupo2.setId(1);
        grupo2.setDescricao("Grupo");

        GrupoProduto grupo3 = new GrupoProduto();
        grupo3.setId(2);
        grupo3.setDescricao("Outro Grupo");

        assertEquals(grupo1, grupo2);
        assertEquals(grupo1.hashCode(), grupo2.hashCode());
        assertNotEquals(grupo1, grupo3);
        assertNotEquals(grupo1.hashCode(), grupo3.hashCode());
    }
}
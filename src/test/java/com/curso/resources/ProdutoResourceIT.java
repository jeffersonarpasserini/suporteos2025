package com.curso.resources;

import com.curso.domains.GrupoProduto;
import com.curso.domains.Produto;
import com.curso.domains.dtos.ProdutoDTO;
import com.curso.domains.enums.Status;
import com.curso.repositories.GrupoProdutoRepository;
import com.curso.repositories.ProdutoRepository;
import com.curso.suporteos2025.Suporteos2025Application;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
@SpringBootTest(classes = Suporteos2025Application.class)
@AutoConfigureMockMvc
@Transactional
class ProdutoResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private GrupoProdutoRepository grupoProdutoRepository;

    private GrupoProduto grupoProduto;
    private Produto produtoCaboHdmi;
    private Produto produtoNotebook;

    @BeforeEach
    void setUp() {
        produtoRepository.deleteAll();
        grupoProdutoRepository.deleteAll();

        GrupoProduto grupo = new GrupoProduto();
        grupo.setDescricao("Informática");
        grupo.setStatus(Status.ATIVO);
        grupoProduto = grupoProdutoRepository.save(grupo);

        Produto caboHdmi = new Produto();
        caboHdmi.setDescricao("Cabo HDMI");
        caboHdmi.setCodigoBarra("1234567890123");
        caboHdmi.setGrupoProduto(grupoProduto);
        caboHdmi.setStatus(Status.ATIVO);
        caboHdmi.setSaldoEstoque(new BigDecimal("5.000"));
        caboHdmi.setValorUnitario(new BigDecimal("39.90"));
        produtoCaboHdmi = produtoRepository.save(caboHdmi);

        Produto notebook = new Produto();
        notebook.setDescricao("Notebook Gamer");
        notebook.setCodigoBarra("7891234567890");
        notebook.setGrupoProduto(grupoProduto);
        notebook.setStatus(Status.ATIVO);
        notebook.setSaldoEstoque(new BigDecimal("2.000"));
        notebook.setValorUnitario(new BigDecimal("5999.00"));
        produtoNotebook = produtoRepository.save(notebook);
    }

    @Test
    @DisplayName("GET /api/produto deve retornar paginação com produtos cadastrados")
    void shouldListProductsWithPagination() throws Exception {
        mockMvc.perform(get("/api/produto")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].idProduto").value(produtoCaboHdmi.getIdProduto()))
                .andExpect(jsonPath("$.content[0].descricao").value("Cabo HDMI"))
                .andExpect(jsonPath("$.content[0].codigoBarra").value("1234567890123"))
                .andExpect(jsonPath("$.content[0].grupoProdutoId").value(grupoProduto.getId()))
                .andExpect(jsonPath("$.content[0].status").value(Status.ATIVO.getId()))
                .andExpect(jsonPath("$.content[0].valorUnitario").value(closeTo(39.90, 0.001)))
                .andExpect(jsonPath("$.content[0].saldoEstoque").value(closeTo(5.0, 0.001)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("GET /api/produto/{id} deve retornar o produto correspondente")
    void shouldRetrieveProductById() throws Exception {
        mockMvc.perform(get("/api/produto/{id}", produtoCaboHdmi.getIdProduto())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduto").value(produtoCaboHdmi.getIdProduto()))
                .andExpect(jsonPath("$.descricao").value("Cabo HDMI"))
                .andExpect(jsonPath("$.codigoBarra").value("1234567890123"))
                .andExpect(jsonPath("$.grupoProdutoId").value(grupoProduto.getId()))
                .andExpect(jsonPath("$.status").value(Status.ATIVO.getId()))
                .andExpect(jsonPath("$.valorUnitario").value(closeTo(39.90, 0.001)))
                .andExpect(jsonPath("$.saldoEstoque").value(closeTo(5.0, 0.001)));
    }

    @Test
    @DisplayName("GET /api/produto/codigobarra/{codigo} deve localizar produto pelo código de barras")
    void shouldRetrieveProductByCodigoBarra() throws Exception {
        mockMvc.perform(get("/api/produto/codigobarra/{codigo}", produtoNotebook.getCodigoBarra())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduto").value(produtoNotebook.getIdProduto()))
                .andExpect(jsonPath("$.descricao").value("Notebook Gamer"))
                .andExpect(jsonPath("$.codigoBarra").value("7891234567890"))
                .andExpect(jsonPath("$.grupoProdutoId").value(grupoProduto.getId()))
                .andExpect(jsonPath("$.status").value(Status.ATIVO.getId()))
                .andExpect(jsonPath("$.valorUnitario").value(closeTo(5999.00, 0.001)))
                .andExpect(jsonPath("$.saldoEstoque").value(closeTo(2.0, 0.001)));
    }

    @Test
    @DisplayName("POST /api/produto deve cadastrar novo produto e retornar Location")
    void shouldCreateProduct() throws Exception {
        var payload = objectMapper.createObjectNode();
        payload.put("descricao", "Mouse Gamer");
        payload.put("codigoBarra", "9998887776661");
        payload.put("grupoProdutoId", grupoProduto.getId());
        payload.put("status", Status.ATIVO.getId());
        payload.put("valorUnitario", 250.00);
        payload.put("saldoEstoque", 3.0);

        MvcResult result = mockMvc.perform(post("/api/produto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/produto/")))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        Long createdId = body.path("idProduto").asLong();

        assertThat(createdId).isNotNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost/api/produto/" + createdId);

        Optional<Produto> saved = produtoRepository.findById(createdId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getDescricao()).isEqualTo("Mouse Gamer");
        assertThat(saved.get().getGrupoProduto().getId()).isEqualTo(grupoProduto.getId());
    }

    @Test
    @DisplayName("POST /api/produto deve retornar 404 quando grupo informado não existir")
    void shouldReturn404WhenCreatingWithUnknownGrupoProduto() throws Exception {
        var payload = objectMapper.createObjectNode();
        payload.put("descricao", "Monitor 4K");
        payload.put("codigoBarra", "5554443332221");
        payload.put("grupoProdutoId", 9999);
        payload.put("status", Status.ATIVO.getId());
        payload.put("valorUnitario", 1800.00);
        payload.put("saldoEstoque", 7.0);

        mockMvc.perform(post("/api/produto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Grupo de Produto não encontrado: id=9999"));
    }

    @Test
    @DisplayName("PUT /api/produto/{id} deve atualizar o produto existente")
    void shouldUpdateProduct() throws Exception {
        var payload = objectMapper.createObjectNode();
        payload.put("idProduto", produtoCaboHdmi.getIdProduto());
        payload.put("descricao", "Cabo HDMI 4K");
        payload.put("codigoBarra", produtoCaboHdmi.getCodigoBarra());
        payload.put("grupoProdutoId", grupoProduto.getId());
        payload.put("status", Status.ATIVO.getId());
        payload.put("valorUnitario", 49.90);
        payload.put("saldoEstoque", 10.0);

        mockMvc.perform(put("/api/produto/{id}", produtoCaboHdmi.getIdProduto())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Cabo HDMI 4K"))
                .andExpect(jsonPath("$.valorUnitario").value(closeTo(49.90, 0.001)))
                .andExpect(jsonPath("$.saldoEstoque").value(closeTo(10.0, 0.001)));

        Produto updated = produtoRepository.findById(produtoCaboHdmi.getIdProduto()).orElseThrow();
        assertThat(updated.getDescricao()).isEqualTo("Cabo HDMI 4K");
        assertThat(updated.getValorUnitario()).isEqualByComparingTo(new BigDecimal("49.90"));
        assertThat(updated.getSaldoEstoque()).isEqualByComparingTo(new BigDecimal("10.000"));
    }

    @Test
    @DisplayName("PUT /api/produto/{id} deve retornar 404 quando produto não existir")
    void shouldReturn404WhenUpdatingUnknownProduct() throws Exception {
        var payload = objectMapper.createObjectNode();
        payload.put("idProduto", 999L);
        payload.put("descricao", "Produto Inexistente");
        payload.put("codigoBarra", "0000000000000");
        payload.put("grupoProdutoId", grupoProduto.getId());
        payload.put("status", Status.ATIVO.getId());
        payload.put("valorUnitario", 10.00);
        payload.put("saldoEstoque", 1.0);

        mockMvc.perform(put("/api/produto/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado: id=999"));
    }

    @Test
    @DisplayName("DELETE /api/produto/{id} deve remover o produto quando existir")
    void shouldDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/produto/{id}", produtoNotebook.getIdProduto()))
                .andExpect(status().isNoContent());

        assertThat(produtoRepository.findById(produtoNotebook.getIdProduto())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/produto/{id} deve retornar 404 quando produto não for encontrado")
    void shouldReturn404WhenDeletingUnknownProduct() throws Exception {
        mockMvc.perform(delete("/api/produto/{id}", 12345L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado: id=12345"));
    }

    @Test
    @DisplayName("POST /api/produto deve criar um produto, persistir no banco e retornar o DTO com os dados esperados")
    void createProdutoShouldPersistAndReturnDto() throws Exception {
        ProdutoDTO payload = new ProdutoDTO();
        payload.setDescricao("Água Mineral");
        payload.setCodigoBarra("7891000100001");
        payload.setGrupoProdutoId(grupoProduto.getId());
        payload.setStatus(Status.ATIVO.getId());
        payload.setValorUnitario(new BigDecimal("5.25"));
        payload.setSaldoEstoque(new BigDecimal("10.000"));
        payload.setValorEstoque(new BigDecimal("52.50"));

        String body = objectMapper.writeValueAsString(payload);

        String response = mockMvc.perform(post("/api/produto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/produto/")))
                .andExpect(jsonPath("$.idProduto").isNumber())
                .andExpect(jsonPath("$.descricao").value("Água Mineral"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ProdutoDTO created = objectMapper.readValue(response, ProdutoDTO.class);

        assertThat(produtoRepository.findById(created.getIdProduto())).isPresent();

        mockMvc.perform(get("/api/produto/{id}", created.getIdProduto()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduto").value(created.getIdProduto()))
                .andExpect(jsonPath("$.descricao").value("Água Mineral"));
    }

    @Test
    @DisplayName("GET /api/produto/codigobarra/{codigo} deve retornar 404 quando produto não for encontrado pelo codigo de barra")
    void getProdutoByCodigoBarraShouldReturnProduto() throws Exception {
        Produto produto = new Produto();
        produto.setDescricao("Refrigerante Lata");
        produto.setCodigoBarra("3216549870123");
        produto.setSaldoEstoque(new BigDecimal("3.000"));
        produto.setValorUnitario(new BigDecimal("6.50"));
        produto.setGrupoProduto(grupoProduto);
        produto.setStatus(Status.ATIVO);
        produtoRepository.save(produto);

        mockMvc.perform(get("/api/produto/codigobarra/{codigo}", produto.getCodigoBarra()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoBarra").value(produto.getCodigoBarra()))
                .andExpect(jsonPath("$.descricao").value("Refrigerante Lata"));
    }
}

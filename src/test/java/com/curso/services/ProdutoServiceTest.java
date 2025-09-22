package com.curso.services;

import com.curso.domains.GrupoProduto;
import com.curso.domains.Produto;
import com.curso.domains.dtos.ProdutoDTO;
import com.curso.domains.enums.Status;
import com.curso.repositories.GrupoProdutoRepository;
import com.curso.repositories.ProdutoRepository;
import com.curso.services.exceptions.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private GrupoProdutoRepository grupoProdutoRepository;

    private ProdutoService service;

    @BeforeEach
    void setUp() {
        service = new ProdutoService(produtoRepository, grupoProdutoRepository);
    }

    @Test
    void findAllShouldReturnMappedDtos() {
        GrupoProduto grupo = buildGrupo(1);
        Produto produto = buildProduto(10L, grupo);
        when(produtoRepository.findAll()).thenReturn(List.of(produto));

        List<ProdutoDTO> result = service.findAll();

        assertEquals(1, result.size());
        ProdutoDTO dto = result.get(0);
        assertEquals(produto.getIdProduto(), dto.getIdProduto());
        assertEquals(produto.getDescricao(), dto.getDescricao());
        assertEquals(grupo.getId(), dto.getGrupoProdutoId());
        assertEquals(produto.getCodigoBarra(), dto.getCodigoBarra());
    }

    @Test
    void findAllPageableShouldClampPageSizeToMaximum() {
        GrupoProduto grupo = buildGrupo(1);
        Produto produto = buildProduto(5L, grupo);
        Page<Produto> page = new PageImpl<>(List.of(produto), PageRequest.of(0, 200), 1);
        when(produtoRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ProdutoDTO> result = service.findAll(PageRequest.of(0, 500));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(produtoRepository).findAll(captor.capture());
        Pageable used = captor.getValue();
        assertEquals(0, used.getPageNumber());
        assertEquals(200, used.getPageSize());
        assertEquals(1, result.getTotalElements());
        assertEquals(produto.getIdProduto(), result.getContent().get(0).getIdProduto());
    }

    @Test
    void findAllByGrupoShouldReturnDtosWhenGrupoExists() {
        int grupoId = 7;
        GrupoProduto grupo = buildGrupo(grupoId);
        Produto produto = buildProduto(11L, grupo);
        Page<Produto> page = new PageImpl<>(List.of(produto), PageRequest.of(2, 150), 1);
        when(grupoProdutoRepository.existsById(grupoId)).thenReturn(true);
        when(produtoRepository.findByGrupoProduto_Id(eq(grupoId), any(Pageable.class))).thenReturn(page);

        Page<ProdutoDTO> result = service.findAllByGrupo(grupoId, PageRequest.of(2, 500));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(produtoRepository).findByGrupoProduto_Id(eq(grupoId), captor.capture());
        Pageable used = captor.getValue();
        assertEquals(2, used.getPageNumber());
        assertEquals(200, used.getPageSize());
        assertEquals(1, result.getContent().size());
        assertEquals(produto.getIdProduto(), result.getContent().get(0).getIdProduto());
    }

    @Test
    void findAllByGrupoUnpagedShouldReturnListContent() {
        int grupoId = 3;
        GrupoProduto grupo = buildGrupo(grupoId);
        Produto produto = buildProduto(20L, grupo);
        Page<Produto> page = new PageImpl<>(List.of(produto));
        when(grupoProdutoRepository.existsById(grupoId)).thenReturn(true);
        when(produtoRepository.findByGrupoProduto_Id(eq(grupoId), any(Pageable.class))).thenReturn(page);

        List<ProdutoDTO> result = service.findAllByGrupo(grupoId);

        assertEquals(1, result.size());
        assertEquals(produto.getCodigoBarra(), result.get(0).getCodigoBarra());
    }

    @Test
    void createShouldPersistAndReturnDto() {
        int grupoId = 4;
        GrupoProduto grupo = buildGrupo(grupoId);
        ProdutoDTO dto = buildDto(null, grupoId);
        Produto saved = buildProduto(99L, grupo);
        when(grupoProdutoRepository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(produtoRepository.save(any(Produto.class))).thenReturn(saved);

        ProdutoDTO result = service.create(dto);

        assertEquals(saved.getIdProduto(), result.getIdProduto());
        assertEquals(saved.getCodigoBarra(), result.getCodigoBarra());
        assertEquals(grupoId, result.getGrupoProdutoId());
    }

    @Test
    void updateShouldPersistAndReturnDto() {
        long id = 41L;
        int grupoId = 5;
        GrupoProduto grupo = buildGrupo(grupoId);
        ProdutoDTO dto = buildDto(id, grupoId);
        Produto saved = buildProduto(id, grupo);
        when(grupoProdutoRepository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(produtoRepository.findById(id)).thenReturn(Optional.of(saved));
        when(produtoRepository.save(any(Produto.class))).thenReturn(saved);

        ProdutoDTO result = service.update(id, dto);

        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).save(captor.capture());
        Produto persisted = captor.getValue();
        assertEquals(id, persisted.getIdProduto());
        assertEquals(saved.getDescricao(), result.getDescricao());
        assertEquals(grupoId, result.getGrupoProdutoId());
    }

    @Test
    void deleteShouldRemoveProdutoWhenItExists() {
        long id = 13L;
        Produto produto = buildProduto(id, buildGrupo(8));
        when(produtoRepository.findById(id)).thenReturn(Optional.of(produto));

        service.delete(id);

        verify(produtoRepository).delete(produto);
    }

    @Test
    void findAllByGrupoShouldThrowWhenGrupoIdIsNull() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.findAllByGrupo(null, Pageable.unpaged()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("grupoId é obrigatório", exception.getReason());
    }

    @Test
    void findAllByGrupoShouldThrowWhenGrupoDoesNotExist() {
        int grupoId = 15;
        when(grupoProdutoRepository.existsById(grupoId)).thenReturn(false);

        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> service.findAllByGrupo(grupoId, Pageable.unpaged()));

        assertEquals("Grupo do produto não encontrado: id=" + grupoId, exception.getMessage());
    }

    @Test
    void createShouldThrowWhenDtoIsNull() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Dados do produto são obrigatórios", exception.getReason());
    }

    @Test
    void findByIdShouldThrowWhenProdutoNotFound() {
        long id = 88L;
        when(produtoRepository.findById(id)).thenReturn(Optional.empty());

        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> service.findById(id));

        assertEquals("Produto não encontrado: id=" + id, exception.getMessage());
    }

    @Test
    void findByCodigoBarraShouldThrowWhenBlank() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.findByCodigoBarra("   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Código de Barra do Produto é obrigatório", exception.getReason());
    }

    @Test
    void deleteShouldThrowWhenIdIsNull() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.delete(null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Id é obrigatório", exception.getReason());
        verify(produtoRepository, never()).delete(any(Produto.class));
    }

    @Test
    void deleteShouldNotInvokeRepositoryWhenProdutoDoesNotExist() {
        long id = 21L;
        when(produtoRepository.findById(id)).thenReturn(Optional.empty());

        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> service.delete(id));

        assertEquals("Produto não encontrado: id=" + id, exception.getMessage());
        verify(produtoRepository, never()).delete(any(Produto.class));
    }

    private GrupoProduto buildGrupo(int id) {
        GrupoProduto grupo = new GrupoProduto();
        grupo.setId(id);
        grupo.setDescricao("Grupo " + id);
        grupo.setStatus(Status.ATIVO);
        return grupo;
    }

    private Produto buildProduto(Long id, GrupoProduto grupo) {
        Produto produto = new Produto();
        produto.setIdProduto(id);
        produto.setCodigoBarra("123456" + id);
        produto.setDescricao("Produto " + id);
        produto.setGrupoProduto(grupo);
        produto.setStatus(Status.ATIVO);
        produto.setSaldoEstoque(new BigDecimal("2.500"));
        produto.setValorUnitario(new BigDecimal("10.00"));
        return produto;
    }

    private ProdutoDTO buildDto(Long id, Integer grupoId) {
        ProdutoDTO dto = new ProdutoDTO();
        dto.setIdProduto(id);
        dto.setDescricao("Produto DTO");
        dto.setCodigoBarra("654321");
        dto.setGrupoProdutoId(grupoId);
        dto.setStatus(1);
        dto.setSaldoEstoque(new BigDecimal("5.000"));
        dto.setValorUnitario(new BigDecimal("20.00"));
        dto.setValorEstoque(new BigDecimal("100.00"));
        return dto;
    }
}

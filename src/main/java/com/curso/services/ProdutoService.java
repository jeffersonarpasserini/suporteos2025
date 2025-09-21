package com.curso.services;

import com.curso.domains.Produto;
import com.curso.domains.dtos.ProdutoDTO;
import com.curso.mappers.ProdutoMapper;
import com.curso.repositories.GrupoProdutoRepository;
import com.curso.repositories.ProdutoRepository;
import com.curso.services.exceptions.ObjectNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProdutoService {

    private static final int MAX_PAGE_SIZE = 200; // limite de segurança

    private final ProdutoRepository produtoRepo;
    private final GrupoProdutoRepository grupoProdutoRepo;

    public ProdutoService(ProdutoRepository produtoRepo,
                          GrupoProdutoRepository grupoProdutoRepo) {
        this.produtoRepo = produtoRepo;
        this.grupoProdutoRepo = grupoProdutoRepo;
    }

    /* =================== READ =================== */

    /** Não paginado, sem filtro */
    @Transactional(readOnly = true)
    public List<ProdutoDTO> findAll() {
        return ProdutoMapper.toDtoList(produtoRepo.findAll());
    }

    /** Paginado, sem filtro (real, no banco) */
    @Transactional(readOnly = true)
    public Page<ProdutoDTO> findAll(Pageable pageable) {
        final Pageable effective;
        if (pageable == null || pageable.isUnpaged()) {
            effective = Pageable.unpaged();
        } else {
            effective = PageRequest.of(
                    Math.max(0, pageable.getPageNumber()),
                    Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                    pageable.getSort()
            );
        }

        Page<Produto> page = produtoRepo.findAll(effective);
        return ProdutoMapper.toDtoPage(page);
    }

    /** Paginado, filtrando por grupo */
    @Transactional(readOnly = true)
    public Page<ProdutoDTO> findAllByGrupo(Integer grupoId, Pageable pageable) {
        if (grupoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grupoId é obrigatório");
        }

        // valida existência do grupo para erro claro
        if (!grupoProdutoRepo.existsById(grupoId)) {
            throw new ObjectNotFoundException("Grupo do produto não encontrado: id=" + grupoId);
        }

        // ✅ trate unpaged aqui
        final Pageable effective;
        if (pageable == null || pageable.isUnpaged()) {
            effective = Pageable.unpaged();
        } else {
            effective = PageRequest.of(
                    Math.max(0, pageable.getPageNumber()),
                    Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                    pageable.getSort()
            );
        }

        Page<Produto> page = produtoRepo.findByGrupoProduto_Id(grupoId, effective);
        return ProdutoMapper.toDtoPage(page);
    }

    /** Não paginado, filtrando por grupo (reaproveita o paginado com unpaged) */
    @Transactional(readOnly = true)
    public List<ProdutoDTO> findAllByGrupo(Integer grupoId) {
        return findAllByGrupo(grupoId, Pageable.unpaged()).getContent();
    }

    @Transactional(readOnly = true)
    public ProdutoDTO findById(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id de Produto é obrigatório");
        }

        return produtoRepo.findById(id)
                .map(ProdutoMapper::toDto)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Produto não encontrado: id=" + id));
    }

    @Transactional(readOnly = true)
    public ProdutoDTO findByCodigoBarra(String codigoBarra) {
        if (codigoBarra == null || codigoBarra.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de Barra do Produto é obrigatório");
        }

        String normalizedCodigoBarra = codigoBarra.trim();

        return produtoRepo.findByCodigoBarra(normalizedCodigoBarra)
                .map(ProdutoMapper::toDto)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Produto não encontrado: Codigo de Barra=" + normalizedCodigoBarra));
    }
}

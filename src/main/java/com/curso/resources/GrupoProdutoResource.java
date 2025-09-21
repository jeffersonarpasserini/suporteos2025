package com.curso.resources;

import com.curso.domains.dtos.GrupoProdutoDTO;
import com.curso.services.GrupoProdutoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupoproduto")
public class GrupoProdutoResource {

    private final GrupoProdutoService service;

    public GrupoProdutoResource(GrupoProdutoService service) {
        this.service = service;
    }

    // GET não paginado (simples e direto)
    @GetMapping("/all")
    public ResponseEntity<List<GrupoProdutoDTO>> listAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // GET "paginado" embrulhado (usa findAll e monta PageImpl)
    @GetMapping
    public ResponseEntity<Page<GrupoProdutoDTO>> list(
            @PageableDefault(size = 20, sort = "descricao") Pageable pageable) {
        List<GrupoProdutoDTO> all = service.findAll();
        Page<GrupoProdutoDTO> page = new PageImpl<>(all, pageable, all.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoProdutoDTO> findById(@PathVariable Integer id) {
        GrupoProdutoDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    
}

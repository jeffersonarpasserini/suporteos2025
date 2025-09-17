package com.curso.services;

import com.curso.domains.dtos.GrupoProdutoDTO;
import com.curso.mappers.GrupoProdutoMapper;
import com.curso.repositories.GrupoProdutoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrupoProdutoService {

    private final GrupoProdutoRepository grupoProdutoRepo;

    // Injeção por construtor (Spring injeta automaticamente se houver só um construtor público)
    public GrupoProdutoService(GrupoProdutoRepository grupoProdutoRepo) {
        this.grupoProdutoRepo = grupoProdutoRepo;
    }

    @Transactional(readOnly = true)
    public List<GrupoProdutoDTO> findAll(){
        //retorna uma lista de ProdutoDTO
        return GrupoProdutoMapper.toDtoList(grupoProdutoRepo.findAll());
    }

}

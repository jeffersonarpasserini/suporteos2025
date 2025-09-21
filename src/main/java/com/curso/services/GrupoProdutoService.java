package com.curso.services;

import com.curso.domains.dtos.GrupoProdutoDTO;
import com.curso.mappers.GrupoProdutoMapper;
import com.curso.repositories.GrupoProdutoRepository;
import com.curso.services.exceptions.ObjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional(readOnly = true)
    public GrupoProdutoDTO findById(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id é obrigatório");
        }

        return grupoProdutoRepo.findById(id)
                .map(GrupoProdutoMapper::toDto)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Grupo de produto não encontrado: id=" + id));
    }

}

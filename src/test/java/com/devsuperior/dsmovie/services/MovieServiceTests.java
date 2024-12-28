package com.devsuperior.dsmovie.services;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.devsuperior.dsmovie.dto.MovieDTO;
import com.devsuperior.dsmovie.entities.MovieEntity;
import com.devsuperior.dsmovie.repositories.MovieRepository;
import com.devsuperior.dsmovie.services.exceptions.DatabaseException;
import com.devsuperior.dsmovie.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dsmovie.tests.MovieFactory;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(SpringExtension.class)
public class MovieServiceTests {

	@InjectMocks
	private MovieService service;

	@Mock
	private MovieRepository repository;

	private Long existingMovieId;
	private Long nonExistingMovieId;
	private Long dependentMovieId;
	private MovieEntity movieEntity;
	private MovieDTO movieDTO;
	private PageImpl<MovieEntity> page;

	@BeforeEach
	void setUp() throws Exception {
		existingMovieId = 1L;
		dependentMovieId = 2L;
		nonExistingMovieId = 100L;

		movieEntity = MovieFactory.createMovieEntity();
		movieEntity.setId(existingMovieId);
		movieDTO = new MovieDTO(movieEntity);
		page = new PageImpl<>(List.of(movieEntity));

		// Configuração para ID existente
		Mockito.when(repository.existsById(existingMovieId)).thenReturn(true);
		Mockito.doNothing().when(repository).deleteById(existingMovieId);

		// Configuração para ID inexistente
		Mockito.when(repository.existsById(nonExistingMovieId)).thenReturn(false);

		// Configuração para ID dependente
		Mockito.when(repository.existsById(dependentMovieId)).thenReturn(true);
		Mockito.doThrow(DataIntegrityViolationException.class).when(repository).deleteById(dependentMovieId);

		// Configuração para findById
		Mockito.when(repository.findById(existingMovieId)).thenReturn(Optional.of(movieEntity));

		// Configuração para save
		Mockito.when(repository.save(any())).thenReturn(movieEntity);

		// Configuração para update
		Mockito.when(repository.getReferenceById(existingMovieId)).thenReturn(movieEntity);

		// Configuração para busca paginada
		Mockito.when(repository.searchByTitle(Mockito.anyString(), Mockito.any(Pageable.class))).thenReturn(page);

	}

	@Test
	public void findAllShouldReturnPagedMovieDTO() {
		Pageable pageable = PageRequest.of(0, 12);
		String title = movieEntity.getTitle();

		Page<MovieDTO> result = service.findAll(title, pageable);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.isEmpty());
		Assertions.assertEquals(1, result.getNumberOfElements());
		Assertions.assertEquals(movieDTO.getTitle(), result.iterator().next().getTitle());
	}

	@Test
	public void findByIdShouldReturnMovieDTOWhenIdExists() {
		MovieDTO result = service.findById(existingMovieId);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(existingMovieId, result.getId());
		Assertions.assertEquals(movieEntity.getTitle(), result.getTitle());
	}

	@Test
	public void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			service.findById(nonExistingMovieId);
		});
	}

	@Test
	public void insertShouldReturnMovieDTO() {
		MovieDTO result = service.insert(movieDTO);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(result.getTitle(), movieDTO.getTitle());
	}

	@Test
	public void updateShouldReturnMovieDTOWhenIdExists() {
		// Simula a atualização esperada da entidade
		movieEntity.setTitle(movieDTO.getTitle());

		// Chama o método de atualização do serviço
		MovieDTO result = service.update(existingMovieId, movieDTO);

		// Verifica o retorno e as interações
		Assertions.assertNotNull(result);
		Assertions.assertEquals(movieDTO.getTitle(), result.getTitle());
		Mockito.verify(repository, Mockito.times(1)).save(movieEntity);
	}

	@Test
	public void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
		// Configuração para simular a exceção
		Mockito.when(repository.getReferenceById(nonExistingMovieId)).thenThrow(EntityNotFoundException.class);

		// Execução e validação
		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			service.update(nonExistingMovieId, movieDTO);
		});

		// Verifica que o save não foi chamado, já que o fluxo falhou antes
		Mockito.verify(repository, Mockito.never()).save(Mockito.any());
	}

	@Test
	public void deleteShouldDoNothingWhenIdExists() {
		Assertions.assertDoesNotThrow(() -> {
			service.delete(existingMovieId);
		});
		Mockito.verify(repository, Mockito.times(1)).deleteById(existingMovieId);
	}

	@Test
	public void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			service.delete(nonExistingMovieId);
		});
	}

	@Test
	public void deleteShouldThrowDatabaseExceptionWhenDependentId() {
		Assertions.assertThrows(DatabaseException.class, () -> {
			service.delete(dependentMovieId);
		});
	}
}

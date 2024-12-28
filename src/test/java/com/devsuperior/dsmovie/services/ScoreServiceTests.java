package com.devsuperior.dsmovie.services;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.devsuperior.dsmovie.dto.MovieDTO;
import com.devsuperior.dsmovie.dto.ScoreDTO;
import com.devsuperior.dsmovie.entities.MovieEntity;
import com.devsuperior.dsmovie.entities.ScoreEntity;
import com.devsuperior.dsmovie.entities.UserEntity;
import com.devsuperior.dsmovie.repositories.MovieRepository;
import com.devsuperior.dsmovie.repositories.ScoreRepository;
import com.devsuperior.dsmovie.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dsmovie.tests.MovieFactory;
import com.devsuperior.dsmovie.tests.UserFactory;

@ExtendWith(SpringExtension.class)
public class ScoreServiceTests {

	@InjectMocks
	private ScoreService service;

	@Mock
	private UserService userService;

	@Mock
	private MovieRepository movieRepository;

	@Mock
	private ScoreRepository repository;

	private MovieEntity movieEntity;
	private Long existingMovieId, nonExistingMovieId;
	private UserEntity userEntity;

	@BeforeEach
	void setUp() {
		movieEntity = MovieFactory.createMovieEntity();
		existingMovieId = movieEntity.getId();
		nonExistingMovieId = 1000L;
		userEntity = UserFactory.createUserEntity();

		// Mock para usuário autenticado
		Mockito.when(userService.authenticated()).thenReturn(userEntity);

		// Mock para filme existente
		Mockito.when(movieRepository.findById(existingMovieId)).thenReturn(Optional.of(movieEntity));

		// Mock para filme inexistente
		Mockito.when(movieRepository.findById(nonExistingMovieId))
				.thenThrow(new ResourceNotFoundException("Recurso não encontrado"));

		// Mock para salvar o ScoreEntity
		Mockito.when(repository.saveAndFlush(Mockito.any())).thenAnswer(invocation -> {
			ScoreEntity score = invocation.getArgument(0);
			movieEntity.getScores().add(score); // Simula adicionar o score ao filme
			return score;
		});

		// Mock para salvar o MovieEntity
		Mockito.when(movieRepository.save(Mockito.any())).thenReturn(movieEntity);
	}

	@Test
	public void saveScoreShouldReturnMovieDTO() {
		// Configuração do DTO de entrada
		ScoreDTO scoreDTO = new ScoreDTO(existingMovieId, 4.5);

		// Execução do método
		MovieDTO result = service.saveScore(scoreDTO);

		// Validações
		Assertions.assertNotNull(result);
		Assertions.assertEquals(movieEntity.getScore(), result.getScore());
		Assertions.assertEquals(movieEntity.getCount(), result.getCount());

		// Verifica interações
		Mockito.verify(userService, Mockito.times(1)).authenticated();
		Mockito.verify(repository, Mockito.times(1)).saveAndFlush(Mockito.any());
		Mockito.verify(movieRepository, Mockito.times(1)).save(movieEntity);
	}

	@Test
	public void saveScoreShouldThrowResourceNotFoundExceptionWhenNonExistingMovieId() {
		// Configuração do DTO de entrada
		ScoreDTO scoreDTO = new ScoreDTO(nonExistingMovieId, 4.5);

		// Execução e validação
		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			service.saveScore(scoreDTO);
		});

		// Verifica que saveAndFlush e save não foram chamados
		Mockito.verify(repository, Mockito.never()).saveAndFlush(Mockito.any());
		Mockito.verify(movieRepository, Mockito.never()).save(Mockito.any());
	}
}

package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.PostService;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Post;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final BarberRepository barberRepository;

    // Note: On n'injecte pas FileStorageService ici, car c'est le Controller
    // qui gère l'upload physique et nous passe juste l'URL (String).
    // C'est une bonne séparation des responsabilités.

    /**
     * Ajouter une photo au portfolio
     */
    @Override
    @Transactional
    public Post addPost(Long barberId, String imageUrl, String description) {
        // 1. Vérifier que le barbier existe
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable avec l'ID : " + barberId));

        // 2. Créer l'objet
        Post post = Post.builder()
                .barber(barber)
                .imageUrl(imageUrl)
                .description(description)
                .likes(0)
                .createdAt(LocalDateTime.now())
                .build();

        // 3. Sauvegarder
        return postRepository.save(post);
    }

    /**
     * Récupérer le portfolio d'un barbier
     */
    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsByBarber(Long barberId) {
        // Vérifier si le barbier existe (optionnel, mais propre)
        if (!barberRepository.existsById(barberId)) {
            throw new ResourceNotFoundException("Barbier introuvable");
        }
        return postRepository.findByBarberIdOrderByCreatedAtDesc(barberId);
    }

    /**
     * Supprimer une photo (Sécurisé)
     */
    @Override
    @Transactional
    public void deletePost(Long postId, Long currentBarberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post introuvable"));

        // SÉCURITÉ : On vérifie que le post appartient bien à celui qui veut le
        // supprimer
        if (!post.getBarber().getId().equals(currentBarberId)) {
            throw new InvalideEntityException("Action non autorisée : Ce post ne vous appartient pas.");
        }

        postRepository.delete(post);
        // Idéalement : appeler FileStorageService.deleteFile(post.getImageUrl()) ici
        // pour nettoyer le disque
    }
}
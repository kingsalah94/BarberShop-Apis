package com.salahtech.BarberShop_Apis.Services.Interfaces;

import java.util.List;

import com.salahtech.BarberShop_Apis.models.Post;

public interface PostService {

    /**
     * Ajouter une photo au portfolio
     */
    Post addPost(Long barberId, String imageUrl, String description);

    /**
     * Récupérer le portfolio d'un barbier
     */
    List<Post> getPostsByBarber(Long barberId);

    /**
     * Supprimer une photo (Sécurisé)
     */
    void deletePost(Long postId, Long currentBarberId);

}

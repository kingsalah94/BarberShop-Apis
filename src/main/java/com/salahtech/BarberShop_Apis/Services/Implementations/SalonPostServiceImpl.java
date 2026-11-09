package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.SalonPostDto;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonPostService;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonPost;
import com.salahtech.BarberShop_Apis.repositories.SalonPostRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SalonPostServiceImpl implements SalonPostService {

    private final SalonPostRepository postRepository;
    private final SalonRepository salonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalonPostDto> listForSalon(Long salonId) {
        return postRepository.findBySalon_IdOrderByCreatedAtDesc(salonId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonPostDto> feed() {
        return postRepository.findTop30ByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public SalonPostDto create(Long salonId, String imageUrl, String caption) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId));
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException("Image requise pour une publication");
        }
        SalonPost p = SalonPost.builder()
                .salon(salon)
                .imageUrl(imageUrl)
                .caption(caption != null && !caption.isBlank() ? caption.trim() : null)
                .build();
        return toDto(postRepository.save(p));
    }

    @Override
    public void delete(Long salonId, Long postId) {
        SalonPost p = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication introuvable: " + postId));
        if (p.getSalon() == null || !p.getSalon().getId().equals(salonId)) {
            throw new BusinessException("Cette publication n'appartient pas au salon " + salonId);
        }
        postRepository.delete(p);
    }

    private SalonPostDto toDto(SalonPost p) {
        return SalonPostDto.builder()
                .id(p.getId())
                .salonId(p.getSalon() != null ? p.getSalon().getId() : null)
                .salonName(p.getSalon() != null ? p.getSalon().getName() : null)
                .imageUrl(p.getImageUrl())
                .caption(p.getCaption())
                .createdAt(p.getCreatedAt())
                .build();
    }
}

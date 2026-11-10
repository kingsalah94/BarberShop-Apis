package com.salahtech.BarberShop_Apis.Services.Implementations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Dtos.LocalityDto;
import com.salahtech.BarberShop_Apis.Dtos.LocalityRefDto;
import com.salahtech.BarberShop_Apis.Dtos.LocalityStatDto;
import com.salahtech.BarberShop_Apis.Enums.LocalityType;
import com.salahtech.BarberShop_Apis.Exceptions.BadRequestException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.models.Locality;
import com.salahtech.BarberShop_Apis.repositories.LocalityRepository;

import lombok.RequiredArgsConstructor;

/**
 * Référentiel Pays › Région › Ville › Quartier : lecture publique, ajouts libres (signalés « à valider »),
 * gestion admin, descendants (filtre du catalogue) et statistiques remontées par zone.
 */
@Service
@RequiredArgsConstructor
public class LocalityService {

    private final LocalityRepository repo;

    // ── Lecture ────────────────────────────────────────────────────────────

    /** Enfants actifs d'un nœud (pays si parentId null). */
    @Transactional(readOnly = true)
    public List<LocalityDto> children(Long parentId) {
        List<Locality> list = parentId == null ? repo.findByParentIsNullOrderByNameAsc() : repo.findByParent_IdOrderByNameAsc(parentId);
        return list.stream().filter(Locality::isActive).map(l -> toDto(l, false)).toList();
    }

    @Transactional(readOnly = true)
    public LocalityDto get(Long id) {
        return toDto(find(id), true);
    }

    /** Chemin pays → nœud (inclus). */
    @Transactional(readOnly = true)
    public List<LocalityRefDto> path(Long id) {
        return id == null ? List.of() : pathOf(find(id));
    }

    public List<LocalityRefDto> pathOf(Locality l) {
        LinkedList<LocalityRefDto> out = new LinkedList<>();
        Locality cur = l;
        int guard = 0;
        while (cur != null && guard++ < 10) {
            out.addFirst(LocalityRefDto.builder().id(cur.getId()).type(cur.getType().name()).name(cur.getName()).build());
            cur = cur.getParent();
        }
        return out;
    }

    /** Ids du nœud et de tous ses descendants (pour filtrer « Niamey » = tous ses quartiers). */
    @Transactional(readOnly = true)
    public Set<Long> selfAndDescendants(Long id) {
        Map<Long, List<Long>> kids = childrenIndex();
        Set<Long> out = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>(List.of(id));
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (out.add(cur)) stack.addAll(kids.getOrDefault(cur, List.of()));
        }
        return out;
    }

    /** Salons rattachés à la zone (descendants inclus). */
    @Transactional(readOnly = true)
    public Set<Long> salonIdsWithin(Long localityId) {
        return new HashSet<>(repo.salonIdsIn(selfAndDescendants(localityId)));
    }

    public Locality find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Localité introuvable : " + id));
    }

    // ── Écriture ───────────────────────────────────────────────────────────

    /**
     * Ajout sous un parent. Doublon (même nom, casse ignorée) → renvoie l'existant.
     * Hors admin : seuls villes et quartiers, marqués « à valider ».
     */
    @Transactional
    public LocalityDto create(Long parentId, String rawName, boolean admin) {
        String name = normalize(rawName);
        Locality parent = parentId == null ? null : find(parentId);
        LocalityType type = parent == null ? LocalityType.COUNTRY : parent.getType().child();
        if (type == null) throw new BadRequestException("Un quartier ne peut pas avoir de sous-zone.");
        if (!admin && (type == LocalityType.COUNTRY || type == LocalityType.REGION)) {
            throw new BadRequestException("Seul un administrateur peut ajouter un pays ou une région.");
        }
        Optional<Locality> existing = repo.findSibling(parentId, name);
        if (existing.isPresent()) {
            Locality l = existing.get();
            if (!l.isActive() && admin) { l.setActive(true); repo.save(l); }
            return toDto(l, true);
        }
        Locality l = repo.save(Locality.builder().type(type).name(name).parent(parent).active(true).suggested(!admin).build());
        return toDto(l, true);
    }

    @Transactional
    public LocalityDto update(Long id, String rawName, Boolean active, Boolean suggested) {
        Locality l = find(id);
        if (rawName != null && !rawName.isBlank()) {
            String name = normalize(rawName);
            repo.findSibling(l.getParent() == null ? null : l.getParent().getId(), name)
                    .filter(o -> !o.getId().equals(id))
                    .ifPresent(o -> { throw new BadRequestException("« " + name + " » existe déjà à ce niveau."); });
            l.setName(name);
        }
        if (active != null) l.setActive(active);
        if (suggested != null) l.setSuggested(suggested);
        return toDto(repo.save(l), true);
    }

    /** Suppression si rien n'y est rattaché ; sinon désactivation (masquée des listes, données conservées). */
    @Transactional
    public boolean deleteOrDeactivate(Long id) {
        Locality l = find(id);
        long salons = countOf(repo.salonsPerLocality()).getOrDefault(id, 0L);
        long users = countOf(repo.usersPerLocality()).getOrDefault(id, 0L);
        if (salons == 0 && users == 0 && !repo.existsByParent_Id(id)) {
            repo.delete(l);
            return true;
        }
        l.setActive(false);
        repo.save(l);
        return false;
    }

    // ── Console admin ──────────────────────────────────────────────────────

    /** Arbre complet à plat avec compteurs directs (inactifs compris). */
    @Transactional(readOnly = true)
    public List<LocalityDto> all() {
        Map<Long, Long> salons = countOf(repo.salonsPerLocality());
        Map<Long, Long> users = countOf(repo.usersPerLocality());
        Map<Long, List<Long>> kids = childrenIndex();
        return repo.findAll().stream()
                .sorted(Comparator.comparing(Locality::getName, String.CASE_INSENSITIVE_ORDER))
                .map(l -> {
                    LocalityDto d = toDto(l, false);
                    d.setChildrenCount((long) kids.getOrDefault(l.getId(), List.of()).size());
                    d.setSalonsCount(salons.getOrDefault(l.getId(), 0L));
                    d.setUsersCount(users.getOrDefault(l.getId(), 0L));
                    return d;
                }).toList();
    }

    /** Statistiques par zone d'un niveau donné, remontées depuis les descendants. */
    @Transactional(readOnly = true)
    public List<LocalityStatDto> stats(LocalityType level) {
        List<Locality> all = repo.findAll();
        Map<Long, Locality> byId = all.stream().collect(Collectors.toMap(Locality::getId, Function.identity()));
        Map<Long, Long> salons = countOf(repo.salonsPerLocality());
        Map<Long, Long> clients = countOf(repo.clientsPerLocality());
        Map<Long, long[]> bookings = new HashMap<>();
        for (Object[] r : repo.bookingsPerLocality(LocalDateTime.now().minusDays(30))) {
            bookings.put((Long) r[0], new long[] { ((Number) r[1]).longValue(), r[2] == null ? 0 : ((Number) r[2]).longValue() });
        }
        Map<Long, LocalityStatDto> out = new LinkedHashMap<>();
        for (Locality l : all) if (l.getType() == level) {
            out.put(l.getId(), LocalityStatDto.builder().id(l.getId()).name(l.getName()).type(level.name())
                    .parentName(l.getParent() != null ? l.getParent().getName() : null).build());
        }
        // Chaque rattachement remonte jusqu'à son ancêtre du niveau demandé.
        Set<Long> touched = new HashSet<>();
        touched.addAll(salons.keySet()); touched.addAll(clients.keySet()); touched.addAll(bookings.keySet());
        for (Long id : touched) {
            Long target = ancestorAt(byId.get(id), level);
            LocalityStatDto s = target == null ? null : out.get(target);
            if (s == null) continue;
            s.setSalons(s.getSalons() + salons.getOrDefault(id, 0L));
            s.setClients(s.getClients() + clients.getOrDefault(id, 0L));
            long[] b = bookings.get(id);
            if (b != null) { s.setBookings(s.getBookings() + b[0]); s.setBookings30d(s.getBookings30d() + b[1]); }
        }
        return out.values().stream()
                .sorted(Comparator.comparingLong(LocalityStatDto::getSalons).reversed()
                        .thenComparing(Comparator.comparingLong(LocalityStatDto::getClients).reversed())
                        .thenComparing(LocalityStatDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // ── Outils ─────────────────────────────────────────────────────────────

    private Long ancestorAt(Locality l, LocalityType level) {
        int guard = 0;
        while (l != null && guard++ < 10) {
            if (l.getType() == level) return l.getId();
            l = l.getParent();
        }
        return null;
    }

    private Map<Long, List<Long>> childrenIndex() {
        Map<Long, List<Long>> kids = new HashMap<>();
        for (Object[] r : repo.idParentPairs()) {
            if (r[1] != null) kids.computeIfAbsent((Long) r[1], k -> new ArrayList<>()).add((Long) r[0]);
        }
        return kids;
    }

    private static Map<Long, Long> countOf(List<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put((Long) r[0], ((Number) r[1]).longValue());
        return m;
    }

    /** Espaces normalisés, initiale en majuscule ; 2 à 120 caractères. */
    static String normalize(String raw) {
        String n = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (n.length() < 2 || n.length() > 120) throw new BadRequestException("Nom de zone invalide (2 à 120 caractères).");
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private LocalityDto toDto(Locality l, boolean withPath) {
        LocalityType child = l.getType().child();
        return LocalityDto.builder()
                .id(l.getId()).type(l.getType().name()).typeLabel(l.getType().label()).name(l.getName())
                .parentId(l.getParent() != null ? l.getParent().getId() : null)
                .active(l.isActive()).suggested(l.isSuggested())
                .childType(child != null ? child.name() : null)
                .path(withPath ? pathOf(l) : null)
                .build();
    }
}

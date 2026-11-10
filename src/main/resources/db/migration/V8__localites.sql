-- Référentiel géographique Pays › Région › Ville › Quartier (liste gérée + ajouts libres).
CREATE TABLE IF NOT EXISTS localities (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(16)  NOT NULL,              -- COUNTRY | REGION | CITY | DISTRICT
    name        VARCHAR(120) NOT NULL,
    parent_id   BIGINT       REFERENCES localities(id) ON DELETE RESTRICT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    suggested   BOOLEAN      NOT NULL DEFAULT FALSE, -- ajouté par un salon / client, à valider par l'admin
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_localities_parent ON localities(parent_id);
-- Pas de doublon (insensible à la casse) sous un même parent.
CREATE UNIQUE INDEX IF NOT EXISTS uk_localities_parent_name
    ON localities (COALESCE(parent_id, 0), lower(name));

ALTER TABLE salons            ADD COLUMN IF NOT EXISTS locality_id BIGINT REFERENCES localities(id);
ALTER TABLE application_users ADD COLUMN IF NOT EXISTS locality_id BIGINT REFERENCES localities(id);

-- ── Données de départ : Niger ──────────────────────────────────────────────
INSERT INTO localities (type, name) VALUES ('COUNTRY', 'Niger');

INSERT INTO localities (type, name, parent_id)
SELECT 'REGION', r, (SELECT id FROM localities WHERE type = 'COUNTRY' AND name = 'Niger')
FROM unnest(ARRAY['Agadez','Diffa','Dosso','Maradi','Niamey','Tahoua','Tillabéri','Zinder']) AS r;

INSERT INTO localities (type, name, parent_id)
SELECT 'CITY', v.city, reg.id
FROM (VALUES
    ('Agadez','Agadez'), ('Agadez','Arlit'), ('Agadez','Tchirozérine'),
    ('Diffa','Diffa'), ('Diffa','Mainé-Soroa'), ('Diffa','N''Guigmi'),
    ('Dosso','Dosso'), ('Dosso','Birni N''Gaouré'), ('Dosso','Dogondoutchi'), ('Dosso','Gaya'),
    ('Maradi','Maradi'), ('Maradi','Tessaoua'), ('Maradi','Dakoro'), ('Maradi','Madarounfa'),
    ('Niamey','Niamey'),
    ('Tahoua','Tahoua'), ('Tahoua','Birni N''Konni'), ('Tahoua','Madaoua'), ('Tahoua','Illéla'),
    ('Tillabéri','Tillabéri'), ('Tillabéri','Kollo'), ('Tillabéri','Say'), ('Tillabéri','Téra'),
    ('Tillabéri','Filingué'), ('Tillabéri','Ouallam'),
    ('Zinder','Zinder'), ('Zinder','Magaria'), ('Zinder','Mirriah'), ('Zinder','Tanout'), ('Zinder','Gouré')
) AS v(region, city)
JOIN localities reg ON reg.type = 'REGION' AND reg.name = v.region;

INSERT INTO localities (type, name, parent_id)
SELECT 'DISTRICT', q, (SELECT c.id FROM localities c JOIN localities r ON r.id = c.parent_id
                        WHERE c.type = 'CITY' AND c.name = 'Niamey' AND r.name = 'Niamey')
FROM unnest(ARRAY[
    'Plateau','Koubia','Lazaret','Yantala','Kouara Kano','Dar Es Salam','Gamkalé','Harobanda',
    'Talladjé','Recasement','Terminus','Banifandou','Boukoki','Madina','Bobiel','Koira Kano',
    'Goudel','Saga','Wadata','Lacouroussou','Abidjan','Zongo','Bandabari','Cité Fayçal','Kalley',
    'Gaweye','Niamey 2000','Tchangarey','Kirkissoye','Aéroport','Sonuci','Gamkalley'
]) AS q;

-- Salons existants : rattachement au quartier (puis à la ville) cité dans leur adresse texte.
UPDATE salons s SET locality_id = (
    SELECT l.id FROM localities l
    WHERE l.type = 'DISTRICT' AND s.address ILIKE '%' || l.name || '%'
    ORDER BY length(l.name) DESC LIMIT 1)
WHERE s.locality_id IS NULL AND s.address IS NOT NULL;

UPDATE salons s SET locality_id = (
    SELECT l.id FROM localities l
    WHERE l.type = 'CITY' AND s.address ILIKE '%' || l.name || '%'
    ORDER BY length(l.name) DESC LIMIT 1)
WHERE s.locality_id IS NULL AND s.address IS NOT NULL;

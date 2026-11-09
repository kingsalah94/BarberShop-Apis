-- ════════════════════════════════════════════════════════════════════════════
--  V2 — Unification « tout salon » : les comptes de l'ancien modèle « barbier »
--  deviennent des prestataires (Employee) rattachés à leur salon.
--  Idempotent ; sans effet sur une base neuve.
-- ════════════════════════════════════════════════════════════════════════════

-- 1. Barbier rattaché à un salon, sans fiche prestataire → création de la fiche.
INSERT INTO employees (user_id, barber_id, salon_id, primary_salon_id, first_name, last_name, email,
                       phone_number, role, is_active, is_available, created_at)
SELECT u.id, b.id, b.salon_id, b.salon_id,
       COALESCE(NULLIF(u.first_name, ''), 'Prestataire'), COALESCE(NULLIF(u.last_name, ''), '-'),
       u.email, u.phone, 'BARBER', TRUE, TRUE, NOW()
FROM barbers b
JOIN application_users u ON u.id = b.user_id
WHERE b.salon_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.user_id = u.id);

-- 2. Fiche prestataire reliée à son profil barbier si besoin.
UPDATE employees e
SET barber_id = b.id
FROM barbers b
WHERE e.barber_id IS NULL AND b.user_id = e.user_id
  AND NOT EXISTS (SELECT 1 FROM employees x WHERE x.barber_id = b.id);

-- 3. Salons du prestataire (table de liaison) : salon principal + salon historique.
INSERT INTO employee_salons (employee_id, salon_id)
SELECT DISTINCT e.id, s.salon_id
FROM employees e
CROSS JOIN LATERAL (VALUES (e.primary_salon_id), (e.salon_id)) AS s(salon_id)
WHERE s.salon_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM employee_salons es WHERE es.employee_id = e.id AND es.salon_id = s.salon_id);

-- 4. Tout prestataire qui a un compte obtient l'accès EMPLOYEE à ses salons
--    (rien n'est écrasé si l'utilisateur y a déjà un rôle : owner, manager...).
INSERT INTO salon_memberships (user_id, salon_id, role, created_at)
SELECT DISTINCT e.user_id, es.salon_id, 'EMPLOYEE', NOW()
FROM employees e
JOIN employee_salons es ON es.employee_id = e.id
WHERE e.user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM salon_memberships m WHERE m.user_id = e.user_id AND m.salon_id = es.salon_id);

-- 5. Type de compte aligné sur le rôle réel.
--    Ancien « BARBER » propriétaire d'un salon → SALON_OWNER.
UPDATE application_users u
SET user_type = 'SALON_OWNER'
WHERE u.user_type = 'BARBER'
  AND EXISTS (SELECT 1 FROM salon_memberships m WHERE m.user_id = u.id AND m.role = 'OWNER');

--    Ancien « BARBER » prestataire d'un salon → EMPLOYEE.
UPDATE application_users u
SET user_type = 'EMPLOYEE'
WHERE u.user_type = 'BARBER'
  AND EXISTS (SELECT 1 FROM salon_memberships m WHERE m.user_id = u.id AND m.role = 'EMPLOYEE');

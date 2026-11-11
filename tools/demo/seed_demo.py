"""
Jeu de données de DÉMO BarberShop (Niger, FCFA) — à lancer sur une base FRAÎCHE (migrations appliquées).

  python tools/demo/seed_demo.py --api http://localhost:8082/api/v1

1) Structure via l'API (mêmes règles métier que l'app) : propriétaires, salons + zone, horaires,
   prestations, prestataires (avec accès), manager, accueil, postes, clients, zones, favoris, message.
2) Historique via SQL (60 jours) : réservations terminées / annulées / absences, file d'attente payée,
   avis, à venir (7 jours), file du jour, notes clients, photos (fichiers déjà présents dans uploads/user).

Mots de passe : Owner@123, Manager@123, Reception@123, Presta@123, Client@123 (admin : .env).
Ne JAMAIS lancer en production.
"""
import argparse
import json
import os
import random
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta

random.seed(2026)

PSQL = r"C:\Program Files\PostgreSQL\17\bin\psql.exe"
DB = dict(host="localhost", user="postgres", db="Test", password="root")
FILE_BASE = "http://localhost:8080/api/uploads/"

# ─────────────────────────────── données ────────────────────────────────

CATEGORIES = [
    ("Coupe", "Coupes homme, dégradés, coupes enfant", "cut-outline"),
    ("Barbe & rasage", "Taille de barbe, rasage traditionnel", "man-outline"),
    ("Soins", "Soins du visage et du cuir chevelu", "sparkles-outline"),
    ("Coiffure afro", "Locks, tresses, twists", "color-wand-outline"),
]

# (nom, description, prix, durée, catégorie, image)
SERVICES = [
    ("Coupe homme", "Coupe aux ciseaux et à la tondeuse, finitions au rasoir.", 3000, 30, "Coupe", "haircut-81c741ea.jpeg"),
    ("Dégradé américain", "Dégradé net (bas, moyen ou haut) et contours précis.", 3500, 35, "Coupe", "5b653c79-7251-4084-9579-5acb21f935d9.jpg"),
    ("Coupe + barbe", "La formule complète : coupe, taille de barbe et serviette chaude.", 5000, 50, "Barbe & rasage", "f02ad674-5d36-49e7-b33b-bda1a7f355d4.jpg"),
    ("Taille de barbe", "Mise en forme et contours de la barbe.", 2000, 20, "Barbe & rasage", "08c5a4b1-1889-4729-a57e-4cec8ebaf80e.webp"),
    ("Rasage traditionnel", "Rasage au coupe-chou, serviette chaude et baume.", 2500, 25, "Barbe & rasage", "hardshave-4ea6f168.webp"),
    ("Coupe enfant", "Pour les moins de 12 ans.", 2000, 20, "Coupe", None),
    ("Soin du visage", "Gommage, masque et hydratation.", 6000, 40, "Soins", None),
    ("Locks — entretien", "Resserrage des racines et soin.", 8000, 90, "Coiffure afro", "3d3cd058-24c3-42e8-8deb-a575a7877fe8.jpg"),
]

SALONS = [
    {
        "key": "plateau", "name": "Le Barbier du Plateau", "address": "Avenue de la Mairie, Plateau, Niamey",
        "lat": 13.5126, "lng": 2.1089, "phone": "+227 90 11 22 33", "district": "Plateau", "city": "Niamey", "region": "Niamey",
        "owner": ("Moussa", "Issoufou", "owner@demo.barbershop", "+227 90 00 00 01"),
        "manager": ("Aïcha", "Mahamadou", "manager@demo.barbershop", "Manager@123"),
        "reception": ("Fati", "Abdou", "receptionist@demo.barbershop", "Reception@123"),
        "staff": [("Ibrahim", "Moussa", "presta.ibrahim@demo.barbershop", "#B5743A"),
                  ("Abdoul", "Karim", "presta.abdoul@demo.barbershop", "#2F7A4F"),
                  ("Yacine", "Touré", "presta.yacine@demo.barbershop", "#3B5BA9")],
        "stations": 4, "price_factor": 1.0, "services": [0, 1, 2, 3, 4, 5, 6, 7],
        "hours": "plateau", "logo": "0b89f9a6-58a4-4605-98cd-b66834b09a6f.webp", "cover": "0ac7524c-12f8-4aca-8285-3ab96d59255c.webp",
        "posts": [("5b653c79-7251-4084-9579-5acb21f935d9.jpg", "Dégradé bas + contours au rasoir"),
                  ("f02ad674-5d36-49e7-b33b-bda1a7f355d4.jpg", "Coupe + barbe sculptée"),
                  ("08c5a4b1-1889-4729-a57e-4cec8ebaf80e.webp", "Barbe taillée, finitions nettes"),
                  ("3d3cd058-24c3-42e8-8deb-a575a7877fe8.jpg", "Entretien des locks")],
        "daily": (5, 9),
    },
    {
        "key": "koubia", "name": "Koubia Style", "address": "Rue KB 25, Koubia, Niamey",
        "lat": 13.5460, "lng": 2.0870, "phone": "+227 96 44 55 66", "district": "Koubia", "city": "Niamey", "region": "Niamey",
        "owner": ("Hadiza", "Boubacar", "owner.koubia@demo.barbershop", "+227 90 00 00 02"),
        "staff": [("Souleymane", "Garba", "presta.souleymane@demo.barbershop", "#8C5524"),
                  ("Ismaël", "Hamani", "presta.ismael@demo.barbershop", "#6B4C9A")],
        "stations": 2, "price_factor": 0.9, "services": [0, 1, 2, 3, 5, 7],
        "hours": "koubia", "logo": None, "cover": "174b4545-02d8-47a1-89df-b62dc40c2999.png",
        "posts": [("5d22ac79-79b5-430d-bbde-4e135dab4397.webp", "Twists courts"),
                  ("ebbe177f-c189-4fc0-bba6-740fcc211a9b.webp", "Dégradé + design"),
                  ("9fc5f430-2922-4c7f-ad76-d498cffb966f.png", "Coupe enfant")],
        "daily": (3, 6),
    },
    {
        "key": "maradi", "name": "Maradi Coupe & Barbe", "address": "Grand marché, Maradi",
        "lat": 13.5000, "lng": 7.1017, "phone": "+227 97 77 88 99", "district": None, "city": "Maradi", "region": "Maradi",
        "owner": ("Sani", "Oumarou", "owner.maradi@demo.barbershop", "+227 90 00 00 03"),
        "staff": [("Habibou", "Lawali", "presta.habibou@demo.barbershop", "#B23A3A"),
                  ("Nafiou", "Chaibou", "presta.nafiou@demo.barbershop", "#2F7A4F")],
        "stations": 2, "price_factor": 0.8, "services": [0, 1, 2, 3, 4, 5],
        "hours": "maradi", "logo": None, "cover": None, "posts": [],
        "daily": (2, 5),
    },
]

HOURS = {
    "plateau": {d: [("08:30", "13:00"), ("15:00", "21:00")] for d in ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"]}
               | {"SUNDAY": [("10:00", "18:00")]},
    "koubia": {d: [("09:00", "20:00")] for d in ["TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]} | {"MONDAY": []},
    "maradi": {d: [("08:00", "20:00")] for d in ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"]} | {"SUNDAY": []},
}
WEEKDAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]

# (prénom, nom, e-mail, téléphone, quartier/ville)
CLIENTS = [
    ("Amadou", "Seyni", "client@demo.barbershop", "+227 91 10 10 10", "Plateau"),
    ("Mariama", "Issa", "mariama.issa@demo.barbershop", "+227 91 20 20 20", "Koubia"),
    ("Oumar", "Diallo", "oumar.diallo@demo.barbershop", "+227 91 30 30 30", "Yantala"),
    ("Fatouma", "Ali", "fatouma.ali@demo.barbershop", "+227 91 40 40 40", "Lazaret"),
    ("Hamidou", "Maïga", "hamidou.maiga@demo.barbershop", "+227 91 50 50 50", "Plateau"),
    ("Rahila", "Sani", "rahila.sani@demo.barbershop", "+227 91 60 60 60", "Maradi"),
    ("Boubacar", "Idé", "boubacar.ide@demo.barbershop", "+227 91 70 70 70", "Kouara Kano"),
    ("Zeinab", "Moussa", "zeinab.moussa@demo.barbershop", "+227 91 80 80 80", "Terminus"),
    ("Ali", "Hassane", "ali.hassane@demo.barbershop", "+227 91 90 90 90", "Koubia"),
    ("Kadidja", "Adamou", "kadidja.adamou@demo.barbershop", "+227 92 10 10 10", "Banifandou"),
    ("Issoufou", "Tanko", "issoufou.tanko@demo.barbershop", "+227 92 20 20 20", "Maradi"),
    ("Salamatou", "Garba", "salamatou.garba@demo.barbershop", "+227 92 30 30 30", "Gamkalé"),
]

REVIEWS = [
    (5, "Toujours impeccable, je ne vais nulle part ailleurs."),
    (5, "Dégradé parfait et accueil au top."),
    (5, "Rapide, propre et précis. Merci !"),
    (4, "Très bon travail, un peu d'attente malgré le rendez-vous."),
    (5, "Le rasage à l'ancienne vaut le détour."),
    (4, "Bonne ambiance, prix corrects."),
    (5, "Mon fils adore venir ici."),
    (3, "Correct, mais j'ai attendu 20 minutes."),
    (5, "Barbe taillée exactement comme demandé."),
    (4, None),
    (5, None),
]
WALK_IN_NAMES = ["Abdourahamane", "Seydou", "Moctar", "Hassan", "Djibo", "Laouali", "Saidou", "Mahamane", "Idrissa", "Garba"]

# ─────────────────────────────── outils ─────────────────────────────────


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None, ok=(200, 201)):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method)
        req.add_header("Content-Type", "application/json")
        if token:
            req.add_header("Authorization", "Bearer " + token)
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                raw = r.read().decode() or "null"
                return json.loads(raw) if raw.strip().startswith(("{", "[")) else raw
        except urllib.error.HTTPError as e:
            msg = e.read().decode(errors="replace")
            raise SystemExit(f"API {method} {path} → {e.code} : {msg[:400]}")

    def login(self, email, password):
        return self.call("POST", "/auth/login", {"email": email, "password": password})["accessToken"]


def sql(query, fetch=True):
    env = dict(os.environ, PGPASSWORD=DB["password"], PGCLIENTENCODING="UTF8")
    args = [PSQL, "-h", DB["host"], "-U", DB["user"], "-d", DB["db"], "-v", "ON_ERROR_STOP=1", "-tA", "-F", "\t"]
    with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False, encoding="utf-8") as f:
        f.write(query)
        path = f.name
    try:
        out = subprocess.run(args + ["-f", path], capture_output=True, text=True, encoding="utf-8", env=env)
        if out.returncode != 0:
            raise SystemExit("SQL : " + out.stderr[:800])
        return [l.split("\t") for l in out.stdout.splitlines() if l.strip()] if fetch else None
    finally:
        os.unlink(path)


def q(v):
    """Littéral SQL."""
    if v is None:
        return "NULL"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, datetime):
        return "'" + v.strftime("%Y-%m-%d %H:%M:%S") + "'"
    return "'" + str(v).replace("'", "''") + "'"


def admin_password():
    env = os.path.join(os.path.dirname(__file__), "..", "..", ".env")
    for line in open(env, encoding="utf-8"):
        if line.startswith("APP_ADMIN_PASSWORD="):
            return line.split("=", 1)[1].strip().strip('"')
    raise SystemExit("APP_ADMIN_PASSWORD absent de .env")


# ─────────────────────────────── 1. API ─────────────────────────────────


def seed_structure(api):
    print("• Catégories")
    sql("INSERT INTO categories (name, description, icon, is_active) VALUES "
        + ", ".join(f"({q(n)}, {q(d)}, {q(i)}, true)" for n, d, i in CATEGORIES) + ";", fetch=False)
    cats = {r[1]: int(r[0]) for r in sql("SELECT id, name FROM categories;")}
    # Rôles des sous-comptes (créés à la volée par l'app, garantis ici).
    sql("INSERT INTO roles (name, description) SELECT v.n, v.d FROM (VALUES ('MANAGER','Manager du salon'),"
        " ('RECEPTIONIST','Réceptionniste du salon')) v(n, d) WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.name = v.n);", fetch=False)

    admin = api.login("admin@barbershop.local", admin_password())

    def loc(name, parent):
        kids = api.call("GET", f"/localities?parentId={parent}" if parent else "/localities")
        return next(k["id"] for k in kids if k["name"] == name)

    niger = loc("Niger", None)
    out = {"cats": cats, "salons": {}, "clients": {}}
    for s in SALONS:
        print(f"• Salon {s['name']}")
        region = loc(s["region"], niger)
        city = loc(s["city"], region)
        zone = loc(s["district"], city) if s["district"] else city
        fn, ln, email, phone = s["owner"]
        api.call("POST", "/users?password=Owner%40123", {"firstName": fn, "lastName": ln, "email": email, "phone": phone,
                                                        "userType": "SALON_OWNER", "isEnabled": True, "isVerified": True,
                                                        "roles": ["SALON_OWNER"]}, admin)
        owner = api.login(email, "Owner@123")
        salon = api.call("POST", "/salons", {"name": s["name"], "address": s["address"], "latitude": s["lat"], "longitude": s["lng"],
                                             "phoneNumber": s["phone"], "localityId": zone}, owner)
        sid = salon["id"]
        api.call("PUT", f"/salons/{sid}/hours", [
            {"dayOfWeek": d, "closed": not HOURS[s["hours"]][d],
             "windows": [{"openAt": a, "closeAt": b} for a, b in HOURS[s["hours"]][d]]} for d in WEEKDAYS], owner)
        services = []
        for idx in s["services"]:
            name, desc, price, dur, cat, img = SERVICES[idx]
            p = int(round(price * s["price_factor"] / 100.0) * 100)
            svc = api.call("POST", f"/salons/{sid}/services", {"name": name, "description": desc, "price": p, "durationMinutes": dur,
                                                               "categoryId": cats[cat], "active": True,
                                                               "imageUrl": FILE_BASE + img if img else None}, owner)
            services.append({"id": svc["id"], "price": p, "dur": dur, "name": name})
        staff = []
        for i, (efn, eln, eemail, color) in enumerate(s["staff"]):
            e = api.call("POST", "/employees", {"firstName": efn, "lastName": eln, "email": eemail, "phoneNumber": f"+227 93 {sid}{i} 00 0{i}",
                                                "role": "BARBER", "salonIds": [sid], "primarySalonId": sid, "calendarColor": color,
                                                "hireDate": str(date.today() - timedelta(days=200 + 90 * i)),
                                                "skillIds": list(cats.values())[: 2 + (i % 3)],
                                                "createLogin": True, "loginPassword": "Presta@123"}, owner)
            staff.append(e["id"])
        for role_key, role in (("manager", "MANAGER"), ("reception", "RECEPTIONIST")):
            if role_key in s:
                mfn, mln, memail, mpw = s[role_key]
                api.call("POST", f"/salons/{sid}/members", {"firstName": mfn, "lastName": mln, "email": memail, "role": role, "password": mpw}, owner)
        stations = []
        for i in range(s["stations"]):
            w = api.call("POST", "/workstations", {"name": f"Fauteuil {i + 1}", "salonId": sid, "displayOrder": i + 1}, owner)
            stations.append(w["id"])
            if i < len(staff):
                api.call("POST", f"/workstations/{w['id']}/assign/{staff[i]}", None, owner)
        if s["key"] == "plateau":  # un congé à venir, pour le planning
            start = date.today() + timedelta(days=9)
            api.call("POST", f"/employees/{staff[1]}/timeoff", {"startDate": str(start), "endDate": str(start + timedelta(days=2)),
                                                                "reason": "Congé familial"}, owner)
        out["salons"][s["key"]] = {"id": sid, "owner_email": email, "services": services, "staff": staff, "stations": stations,
                                   "conf": s, "zone": zone}

    print("• Clients")
    niamey_city = loc("Niamey", loc("Niamey", niger))
    maradi_city = loc("Maradi", loc("Maradi", niger))
    for fn, ln, email, phone, zone_name in CLIENTS:
        api.call("POST", "/auth/register", {"firstName": fn, "lastName": ln, "email": email, "password": "Client@123",
                                            "phone": phone, "userType": "CLIENT"})
        tok = api.login(email, "Client@123")
        zone = maradi_city if zone_name == "Maradi" else loc(zone_name, niamey_city)
        api.call("PUT", "/me/locality", {"localityId": zone}, tok)
        out["clients"][email] = {"token": tok, "zone": zone_name}
    me = out["clients"]["client@demo.barbershop"]["token"]
    for key in ("plateau", "koubia"):
        api.call("PUT", f"/me/favorite-salons/{out['salons'][key]['id']}", None, me)
    owner_uid = int(sql("SELECT id FROM application_users WHERE email = 'owner@demo.barbershop';")[0][0])
    api.call("POST", "/messaging/conversations", {"participantId": owner_uid, "salonId": out["salons"]["plateau"]["id"],
                                                  "initialMessage": "Bonjour, est-ce possible de passer samedi vers 10h pour un dégradé ?"}, me)
    return out


# ───────────────────────────── 2. Historique SQL ─────────────────────────


def open_windows(conf, d):
    return HOURS[conf["hours"]][WEEKDAYS[d.weekday()]]


def seed_history(st):
    now = datetime.now().replace(second=0, microsecond=0)
    today = now.date()
    clients = {r[1]: int(r[0]) for r in sql("SELECT c.id, u.email FROM clients c JOIN application_users u ON u.id = c.user_id;")}
    regular = [e for e in clients if e != "client@demo.barbershop"]
    values_b, queue, reviews_for = [], [], []
    upcoming = []

    def pick_client(salon_key):
        if salon_key == "maradi":
            pool = [e for e, c in clients.items() if any(x[2] == e and x[4] == "Maradi" for x in CLIENTS)]
            pool = pool or regular
        else:
            pool = [e for e in regular if not any(x[2] == e and x[4] == "Maradi" for x in CLIENTS)]
        return random.choice(pool)

    for key, s in st["salons"].items():
        conf = s["conf"]
        lo, hi = conf["daily"]
        demo_visits = 0
        for back in range(60, -8, -1):
            d = today - timedelta(days=back)
            wins = open_windows(conf, d)
            if not wins:
                continue
            n = random.randint(lo, hi) if back > 0 else random.randint(2, 4)
            for emp_i, emp in enumerate(s["staff"]):
                # Créneaux sans chevauchement par prestataire.
                cursor = None
                for _ in range(max(1, n // len(s["staff"]) + random.randint(0, 1))):
                    w = random.choice(wins)
                    open_h = datetime.combine(d, datetime.strptime(w[0], "%H:%M").time())
                    close_h = datetime.combine(d, datetime.strptime(w[1], "%H:%M").time())
                    start = max(cursor or open_h, open_h) + timedelta(minutes=random.choice([0, 15, 30, 45, 60, 90]))
                    svc = random.choice(s["services"])
                    end = start + timedelta(minutes=svc["dur"])
                    if end > close_h:
                        continue
                    cursor = end + timedelta(minutes=10)
                    # Le client démo cumule 11 visites payées au Plateau (1 visite offerte disponible).
                    if key == "plateau" and back > 0 and demo_visits < 11 and random.random() < 0.35:
                        email = "client@demo.barbershop"
                        demo_visits += 1
                        status = "COMPLETED"
                    else:
                        email = pick_client(key)
                        if back > 0 or (back == 0 and end <= now):
                            r = random.random()
                            status = "COMPLETED" if r < 0.85 else ("CANCELLED" if r < 0.93 else "NO_SHOW")
                        else:
                            status = "CONFIRMED" if random.random() < 0.7 else "PENDING"
                    created = start - timedelta(days=random.randint(1, 6), hours=random.randint(0, 8))
                    station = s["stations"][emp_i] if emp_i < len(s["stations"]) else None
                    values_b.append(dict(salon=s["id"], client=clients[email], email=email, offering=svc["id"], emp=emp, ws=station,
                                         start=start, end=end, status=status, price=svc["price"], created=created))
        # File du jour : passages sans rendez-vous.
        if key in ("plateau", "koubia") and open_windows(conf, today):
            for i, (state, offset) in enumerate([("COMPLETED", -150), ("COMPLETED", -95), ("IN_CHAIR", -15),
                                                 ("WAITING", -12), ("WAITING", -6), ("WAITING", -2)]):
                if key == "koubia" and i in (1, 4):
                    continue
                svc = random.choice(s["services"])
                t = now + timedelta(minutes=offset)
                queue.append(dict(salon=s["id"], name=random.choice(WALK_IN_NAMES), state=state, checkin=t,
                                  started=t + timedelta(minutes=8) if state in ("COMPLETED", "IN_CHAIR") else None,
                                  ended=t + timedelta(minutes=8 + svc["dur"]) if state == "COMPLETED" else None,
                                  emp=s["staff"][i % len(s["staff"])] if state != "WAITING" else None,
                                  ws=s["stations"][i % len(s["stations"])] if state != "WAITING" else None,
                                  price=svc["price"], day=today))

    print(f"• {len(values_b)} réservations")
    rows = ",\n".join(
        f"({b['salon']}, {b['client']}, {b['offering']}, {b['emp']}, {q(b['ws'])}, {q(b['start'])}, {q(b['end'])}, {q(b['status'])},"
        f" {b['price']}, {q(b['created'])}, {q(b['created'])})" for b in values_b)
    ids = sql("INSERT INTO salon_bookings (salon_id, client_id, offering_id, employee_id, workstation_id, start_at, end_at, status,"
              " total_price, created_at, updated_at) VALUES\n" + rows + "\nRETURNING id;")
    for b, (bid,) in zip(values_b, ids):
        b["id"] = int(bid)

    # File d'attente : chaque visite terminée est passée par la file et a été payée.
    tickets = {}
    qrows = []
    for b in sorted(values_b, key=lambda x: x["start"]):
        if b["status"] != "COMPLETED":
            continue
        k = (b["salon"], b["start"].date())
        tickets[k] = tickets.get(k, 0) + 1
        method = random.choices(["CASH", "MOBILE_MONEY", "CREDIT_CARD"], [6, 3, 1])[0]
        qrows.append(f"({b['salon']}, {b['client']}, (SELECT first_name || ' ' || last_name FROM clients WHERE id = {b['client']}),"
                     f" 'COMPLETED', {q('R-%03d' % tickets[k])}, 1, {q(b['start'] - timedelta(minutes=5))}, {q(b['start'])},"
                     f" {q(b['start'])}, {q(b['end'])}, {b['emp']}, {q(b['ws'])}, {b['price']}, {b['price']}, {q(method)}, 'PAID', {b['id']})")
    for e in queue:
        k = (e["salon"], e["day"])
        tickets[k] = tickets.get(k, 0) + 1
        paid = e["state"] == "COMPLETED"
        qrows.append(f"({e['salon']}, NULL, {q(e['name'])}, {q(e['state'])}, {q('A-%03d' % tickets[k])}, 1, {q(e['checkin'])},"
                     f" {q(e['started'])}, {q(e['started'])}, {q(e['ended'])}, {q(e['emp'])}, {q(e['ws'])}, {e['price']},"
                     f" {e['price'] if paid else 'NULL'}, {q('CASH' if paid else None)}, {q('PAID' if paid else 'PENDING')}, NULL)")
    sql("INSERT INTO queue_entries (salon_id, client_id, customer_name, status, ticket_number, number_of_people, check_in_time,"
        " called_at, started_at, ended_at, assigned_employee_id, workstation_id, expected_price, amount, payment_method,"
        " payment_status, salon_booking_id) VALUES\n" + ",\n".join(qrows) + ";", fetch=False)
    print(f"• {len(qrows)} passages en file (dont {len(queue)} aujourd'hui)")

    # Avis : ~40 % des visites terminées.
    done = [b for b in values_b if b["status"] == "COMPLETED" and b["start"] < now]
    picked = random.sample(done, k=int(len(done) * 0.4))
    rrows = []
    for b in picked:
        rating, comment = random.choice(REVIEWS)
        posted = min(b['end'] + timedelta(hours=random.randint(1, 30)), now - timedelta(minutes=5))  # jamais dans le futur
        rrows.append(f"({b['salon']}, {b['client']}, {b['id']}, {rating}, {q(comment)}, {q(posted)})")
    sql("INSERT INTO salon_reviews (salon_id, client_id, salon_booking_id, overall_rating, comment, created_at) VALUES\n"
        + ",\n".join(rrows) + ";", fetch=False)
    print(f"• {len(rrows)} avis")

    # Notes internes, photos, logos, fin des changements de mot de passe imposés (comptes démo).
    plateau = st["salons"]["plateau"]
    owner_uid = sql("SELECT id FROM application_users WHERE email = 'owner@demo.barbershop';")[0][0]
    sql(f"""
INSERT INTO salon_client_notes (salon_id, client_id, note, updated_by_user_id, updated_at) VALUES
 ({plateau['id']}, {clients['client@demo.barbershop']}, 'Client fidèle. Préfère Ibrahim, dégradé bas, peau sensible : pas d''après-rasage alcoolisé.', {owner_uid}, now()),
 ({plateau['id']}, {clients['hamidou.maiga@demo.barbershop']}, 'Vient souvent le samedi matin avec son fils.', {owner_uid}, now());
""" + "".join(
        f"UPDATE salons SET logo_url = {q(FILE_BASE + s['conf']['logo'] if s['conf']['logo'] else None)},"
        f" cover_url = {q(FILE_BASE + s['conf']['cover'] if s['conf']['cover'] else None)} WHERE id = {s['id']};\n"
        + "".join(f"INSERT INTO salon_posts (salon_id, image_url, caption, created_at) VALUES ({s['id']}, {q(FILE_BASE + img)}, {q(cap)},"
                  f" now() - interval '{3 * (i + 1)} days');\n" for i, (img, cap) in enumerate(s['conf']['posts']))
        for s in st["salons"].values())
        + "UPDATE application_users SET must_change_password = false, password_changed_at = now(), is_verified = true"
          " WHERE email LIKE '%@demo.barbershop';\n", fetch=False)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--api", default="http://localhost:8082/api/v1")
    a = ap.parse_args()
    if int(sql("SELECT count(*) FROM salons;")[0][0]) > 0:
        raise SystemExit("La base contient déjà des salons : ce script s'utilise sur une base fraîche.")
    api = Api(a.api)
    st = seed_structure(api)
    seed_history(st)
    print("✓ Démo prête.")


if __name__ == "__main__":
    sys.exit(main())

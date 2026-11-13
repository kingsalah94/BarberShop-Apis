# Mise en production BarberShop

Un seul serveur (VPS Linux) fait tourner tout le service dans Docker : base de données, API, site web et HTTPS.

```
Internet ──443──▶ Caddy (HTTPS auto) ──/api/*──▶ API Spring Boot ──▶ PostgreSQL
                                     └──  /   ──▶ Site Angular (nginx)
Application mobile ──▶ https://DOMAINE/api  (REST + WebSocket /api/ws + push Firebase)
```

## 1. Ce qu'il faut avant de commencer

| Élément | Détail |
|---|---|
| Serveur | VPS Ubuntu 22.04/24.04, **2 vCPU, 4 Go de RAM** au minimum, 40 Go de disque |
| Nom de domaine | Un enregistrement **A** `barbershop.exemple.com` qui pointe vers l'IP du serveur |
| Pare-feu | Ports **80 et 443** ouverts ; tout le reste fermé (22 pour SSH) |
| Clé Firebase serveur | Le fichier de compte de service (déjà utilisé en dev) |

## 2. Installer le serveur (une seule fois)

```bash
curl -fsSL https://get.docker.com | sh
sudo mkdir -p /opt/barbershop && sudo chown $USER /opt/barbershop
cd /opt/barbershop
git clone <url>/BarberShop-Apis.git      # branche à déployer
git clone <url>/barbershop-web.git
```

## 3. Configurer

```bash
cd /opt/barbershop/BarberShop-Apis/deploy
cp .env.prod.example .env
nano .env                               # DOMAIN, ACME_EMAIL, mots de passe…
openssl rand -base64 32                 # → DB_PASSWORD
openssl rand -base64 64 | tr -d '\n'    # → JWT_SECRET_BASE64 (nouveau, pas celui du dev)
mkdir -p secrets
# copier la clé Firebase serveur (scp depuis ton PC) :
#   scp firebase-barberService-account.json serveur:/opt/barbershop/BarberShop-Apis/deploy/secrets/firebase.json
chmod 600 .env secrets/firebase.json
```

## 4. Démarrer

`COMPOSE_BAKE=false` : les versions récentes de Docker Compose refusent sinon de construire le site, dont le dossier est à côté de celui-ci.

```bash
COMPOSE_BAKE=false docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f api      # attendre « Started BarberShopApisApplication »
```

Vérifier : `https://DOMAINE` (site) et `https://DOMAINE/api/actuator/health` → `{"status":"UP"}`.

Le premier démarrage crée le schéma (Flyway), les rôles, les localités du Niger et le compte **admin@barbershop.local** (mot de passe `APP_ADMIN_PASSWORD`).
Ensuite, dans la console admin : créer les **types d'établissement** et les **catégories**, puis les salons et leurs propriétaires.

## 5. Mettre à jour

```bash
cd /opt/barbershop/BarberShop-Apis && git pull
cd ../barbershop-web && git pull
cd ../BarberShop-Apis/deploy
COMPOSE_BAKE=false docker compose -f docker-compose.prod.yml up -d --build
```
Les migrations de base (Flyway) s'appliquent toutes seules au démarrage. **Ne jamais modifier une migration déjà appliquée** : en ajouter une nouvelle (`V11__…sql`).

## 6. Sauvegardes

Le service `backup` fait un `pg_dump` par jour (14 jours gardés) dans le volume `db-backups`.

```bash
# lister
docker compose -f docker-compose.prod.yml exec backup ls -lh /backups
# copier hors du serveur (à faire régulièrement !)
docker compose -f docker-compose.prod.yml cp backup:/backups ./sauvegardes
# restaurer
gunzip -c barbershop-AAAA-MM-JJ-HHMM.sql.gz | docker compose -f docker-compose.prod.yml exec -T db psql -U barbershop barbershop
```
Les photos téléversées sont dans le volume `uploads-data` : à sauvegarder aussi (`docker run --rm -v barbershop_uploads-data:/u -v $PWD:/b alpine tar czf /b/uploads.tgz -C /u .`).

## 7. Application mobile (Play Store)

Une seule fois, sur ton PC, créer la clé de signature (à **garder précieusement** : sans elle, plus de mise à jour possible sur le Play Store) :

```powershell
keytool -genkey -v -keystore C:\cles\barbershop-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```
puis créer `barbershop-mobile-new/android/key.properties` :
```
storeFile=C:\\cles\\barbershop-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

Compiler :
```powershell
cd C:\MyProjects\BarberShop\barbershop-mobile-new
.\tools\build-release.ps1 -Domain barbershop.exemple.com        # AAB pour le Play Store
.\tools\build-release.ps1 -Domain barbershop.exemple.com -Apk   # APK à installer directement
```

Connexion Google en release : ajouter l'empreinte **SHA-1 de la clé release** (`keytool -list -v -keystore C:\cles\barbershop-upload.jks -alias upload`) dans la console Google Cloud (client OAuth Android) **et** dans Firebase (Paramètres du projet › votre appli Android). Si l'appli est publiée sur le Play Store, ajouter aussi le SHA-1 de « Signature d'application » affiché par la Play Console.

## 8. Avant d'ouvrir au public — check-list

- [ ] `.env` de prod : mots de passe **nouveaux** (jamais ceux du dev), `chmod 600`
- [ ] Mot de passe Gmail d'application et identifiants Twilio **régénérés** (les anciens ont été dans git)
- [ ] `https://DOMAINE` répond avec un certificat valide ; `http://` redirige vers `https://`
- [ ] Connexion admin OK, puis changement du mot de passe admin
- [ ] Un salon de test : réservation depuis le web et le mobile, notification reçue (appli fermée)
- [ ] Sauvegarde du jour présente dans `/backups`, et une copie hors du serveur
- [ ] Clé de signature mobile (`.jks`) + `key.properties` sauvegardés hors du PC

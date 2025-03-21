# Garuda - Guide d'Installation et de Configuration

## Qu'est-ce que Garuda ?

Garuda est un outil léger développé en Scala avec le framework Play qui permet de collecter des messages en temps réel depuis différentes plateformes de médias sociaux comme Twitter/X et Bluesky. L'application propose plusieurs modules pour traiter les données collectées, notamment leur stockage dans une base de données PostgreSQL.

## Prérequis

Pour installer et exécuter Garuda, vous aurez besoin de :

- [Docker Engine](https://docs.docker.com/get-docker/) (version 19.03.0+)
- [Docker Compose](https://docs.docker.com/compose/install/) (version 1.27.0+)
- Au moins un compte développeur sur les plateformes supportées (Twitter/X, Bluesky)

## Installation et lancement

### Option 1 : Utilisation du package précompilé

1. Téléchargez le fichier zip dans la section "releases" du projet
2. Décompressez l'archive
3. Exécutez le script approprié dans le dossier `bin` :
   - `./bin/garuda` (Linux)
   - `bin\garuda.bat` (Windows)

L'application sera accessible à l'adresse [http://localhost:9000](http://localhost:9000)

### Option 2 : Installation avec Docker

#### 1. Lancement des dépendances

```bash
# Dans le répertoire docker
cd docker
docker compose up --force-recreate -d
```

Cette commande lancera un conteneur PostgreSQL avec deux bases de données :
- `garudadef` : utilisée pour définir les collectes
- `garuda` : base de données cible qui recevra les messages collectés

#### 2. Construction et lancement de l'application

```bash
# Construction de l'image Docker
docker build . -f docker/garuda/Dockerfile -t garuda:latest

# Lancement de l'application
docker run --rm --name garuda \
  --network host \
  -u $(id -u):$(id -g) \
  -v "$(pwd)/somewhere/collects":"/opt/garuda/Collects" \
  -v "$(pwd)/somewhere/logs":"/opt/garuda/logs" \
  -e JAVA_OPTS="\
    -Dplay.http.secret.key=\"une-clé-secrète-à-changer\" \
    -Dgaruda.directory=\"Collects\" \
    -Dslick.dbs.default.db.url=jdbc:postgresql://localhost:5432/garudadef?user=garudadef&password=garudadef\
  " \
  garuda:latest
```

### Option 3 : Compilation et exécution depuis les sources

```bash
# Création des répertoires de cache
mkdir -p ${HOME}/{.cache,.ivy2,.sbt}

# Compilation et lancement avec SBT via Docker
docker run -it --rm --name garuda \
  --network host \
  -u $(id -u):$(id -g) \
  -v /etc/passwd:/etc/passwd:ro \
  -v "${HOME}/.cache":"${HOME}/.cache" \
  -v "${HOME}/.ivy2":"${HOME}/.ivy2" \
  -v "${HOME}/.sbt":"${HOME}/.sbt" \
  -v "$(pwd)":/app -w /app \
  sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.17_8_1.8.2_2.13.10 \
  sbt clean compile run -Dslick.dbs.default.profile="slick.jdbc.PostgresProfile$" \
  -Dslick.dbs.default.db.driver="org.postgresql.Driver" \
  -Dslick.dbs.default.db.url="jdbc:postgresql://localhost:5432/garudadef?user=garudadef&password=garudadef"
```

## Configuration

### Configuration de l'application

Le fichier principal de configuration se trouve dans `conf/application.conf`. Voici les principales options à modifier :

- `garuda.directory` : le répertoire où seront stockés les fichiers JSON contenant les messages collectés
- `slick.dbs.default.db.url` : l'URL de connexion à la base de données
- `play.http.secret.key` : la clé secrète pour la sécurité de l'application
- `providers.twitter.api.*` et `providers.bluesky.api.*` : les informations d'authentification pour les APIs

### Guide détaillé du fichier de configuration

Le fichier `conf/application.conf` est structuré en plusieurs sections qui permettent de personnaliser tous les aspects de l'application :

#### Configuration générale

```
application.name = "Garuda"
application.environment = "dev"  # environnement (dev, prod, test)
application.secret = "votre-clé-secrète"
application.langs = ["fr"]
application.timezone = "Europe/Paris"
```

#### Configuration de la base de données

```
slick.dbs.default.profile = "slick.jdbc.PostgresProfile$"  # Type de base de données
slick.dbs.default.db.driver = "org.postgresql.Driver"      # Driver JDBC
slick.dbs.default.db.url = "jdbc:postgresql://localhost:5432/garuda"  # URL de connexion
slick.dbs.default.db.user = "garuda_user"                  # Utilisateur
slick.dbs.default.db.password = "garuda_password"          # Mot de passe
```

#### Configuration des APIs

##### Twitter/X API
```
providers.twitter.enabled = true                           # Activer/désactiver l'API Twitter
providers.twitter.api.key = "votre-clé-api"                # Clé API Twitter
providers.twitter.api.secret = "votre-secret-api"          # Secret API
providers.twitter.api.bearer_token = "votre-bearer-token"  # Token d'authentification
providers.twitter.api.app_id = "votre-app-id"              # ID de l'application
providers.twitter.api.app_name = "nom-de-votre-app"        # Nom de l'application
providers.twitter.api.rate_limit = 100                     # Limite de requêtes par minute
providers.twitter.api.timeout = 30s                        # Timeout des requêtes
```

##### Bluesky API
```
providers.bluesky.enabled = true                           # Activer/désactiver l'API Bluesky
providers.bluesky.api.url = "https://bsky.social"          # URL de l'API Bluesky
providers.bluesky.api.identifier = "votre-identifiant"     # Identifiant Bluesky
providers.bluesky.api.password = "votre-mot-de-passe"      # Mot de passe
providers.bluesky.api.rate_limit = 100                     # Limite de requêtes par minute
providers.bluesky.api.timeout = 30s                        # Timeout des requêtes
```

#### Configuration du module PostgreSQL

```
modules.postgres.enabled = true                            # Activer/désactiver le module
modules.postgres.batch.size = 100                          # Taille des lots d'insertion
modules.postgres.flush.interval = "5s"                     # Intervalle de flush des données
modules.postgres.schema = "public"                         # Schéma PostgreSQL

# Noms des tables dans la base de données
modules.postgres.tables {
  message = "social_message"
  tweet = "tweet"
  bluesky_post = "bluesky_post"
  # ... autres tables ...
}
```

#### Configuration du cache

```
cache {
  defaultTtl = 1h                                          # Durée de vie par défaut des entrées
  maxSize = 1000                                           # Taille maximale du cache
}
```

#### Configuration des logs

```
logger.application = INFO                                  # Niveau de log de l'application
logger.providers = INFO                                    # Niveau de log des providers
logger.modules = INFO                                      # Niveau de log des modules
logger.dao = INFO                                          # Niveau de log de la couche DAO
```

#### Configuration des fichiers

```
garuda.directory = "Collects"                              # Répertoire des collectes
garuda.temp.directory = "tmp"                              # Répertoire temporaire
garuda.max.file.size = 100MB                               # Taille maximale des fichiers
garuda.allowed.file.types = ["json", "csv", "txt"]         # Types de fichiers autorisés
```

#### Configuration de la sécurité

```
play.http.secret.key = "votre-clé-secrète"                 # Clé secrète pour les sessions
play.http.session.secure = true                            # Sessions sécurisées (HTTPS)
play.http.session.httpOnly = true                          # Cookies accessibles uniquement par HTTP
play.http.session.maxAge = 7d                              # Durée maximale des sessions
```

### Configuration de Docker

Si vous utilisez Docker, vous pouvez modifier la configuration en passant des variables d'environnement :

```bash
-e JAVA_OPTS="\
  -Dplay.http.secret.key=\"votre-clé-secrète\" \
  -Dgaruda.directory=\"Collects\" \
  -Dslick.dbs.default.db.url=jdbc:postgresql://localhost:5432/garudadef?user=garudadef&password=garudadef\
"
```

## Extension de l'application

### Ajout d'une nouvelle API de réseau social

Garuda est conçu avec une architecture extensible qui permet d'ajouter facilement le support pour de nouvelles plateformes de médias sociaux. Voici les étapes à suivre pour intégrer une nouvelle API :

#### 1. Déclarer un nouveau type de Provider

Dans le fichier `app/providers/ProviderType.scala`, ajoutez un nouveau cas pour votre plateforme :

```scala
case object NouvelleAPI extends ProviderType {
  val id = "nouvelle-api"
  val name = "Nouvelle API"
  val description = "Description de la nouvelle API"
}

// Ajoutez-le également à la méthode fromId
def fromId(id: String): Option[ProviderType] = id match {
  case Twitter.id => Some(Twitter)
  case Bluesky.id => Some(Bluesky)
  case NouvelleAPI.id => Some(NouvelleAPI)  // Nouvelle ligne
  case _ => None
}

// Et à la liste des valeurs
def values: Seq[ProviderType] = Seq(Twitter, Bluesky, NouvelleAPI)  // Ajoutez NouvelleAPI
```

#### 2. Créer un package pour votre API

Créez un nouveau package dans `app/providers` pour votre API (ex: `app/providers/nouvelleapi/`).

#### 3. Implémenter les classes nécessaires

Votre package devrait contenir au minimum les classes suivantes :

1. **Provider principal** (`NouvelleAPIProvider.scala`) :

```scala
package providers.nouvelleapi

import providers.{ProviderType, SocialMediaProvider}
import models.{SocialAccount, SocialCollect}

class NouvelleAPIProvider extends SocialMediaProvider {
  override def providerType: ProviderType = ProviderType.NouvelleAPI
  
  override def createConnection(account: SocialAccount): NouvelleAPIConnection = {
    new NouvelleAPIConnection(account)
  }
  
  // Implémentez toutes les méthodes requises par l'interface SocialMediaProvider
  // ...
}
```

2. **Classe de connexion** (`NouvelleAPIConnection.scala`) :

```scala
package providers.nouvelleapi

import providers.{StreamingConnection, MessageListener}
import models.SocialAccount

class NouvelleAPIConnection(account: SocialAccount) extends StreamingConnection {
  // Gérez ici la connexion à l'API, l'authentification, etc.
  // ...
  
  override def connect(): Either[String, StreamingConnection] = {
    // Logique de connexion
    Right(this)
  }
  
  override def disconnect(): Boolean = {
    // Logique de déconnexion
    true
  }
  
  // Implémentez les autres méthodes requises
  // ...
}
```

3. **Classe pour les règles** (`NouvelleAPIRule.scala`) :

```scala
package providers.nouvelleapi

import providers.SocialMediaRule
import play.api.libs.json._

case class NouvelleAPIRule(tag: String, content: String, id: Option[String] = None) extends SocialMediaRule {
  override def getRuleId: Option[String] = id
  override def getTag: String = tag
  override def getContent: String = content
}

object NouvelleAPIRule {
  // Convertisseurs JSON si nécessaire
}
```

4. **Listener de flux** (`NouvelleAPIStreamListener.scala`) :

```scala
package providers.nouvelleapi

import providers.MessageListener

class NouvelleAPIStreamListener(messageHandler: String => Unit) {
  // Implémentez la logique pour écouter le flux de messages
  // et appeler messageHandler pour chaque message reçu
}
```

#### 4. Enregistrer votre Provider

Dans la classe d'initialisation de l'application (par exemple dans un module Play), enregistrez votre nouveau provider :

```scala
import providers.{ProviderRegistry, ProviderType}
import providers.nouvelleapi.NouvelleAPIProvider

// Lors de l'initialisation de l'application
ProviderRegistry.register(
  ProviderType.NouvelleAPI,
  () => new NouvelleAPIProvider()
)
```

#### 5. Ajouter la configuration

Dans `conf/application.conf`, ajoutez une section pour votre nouvelle API :

```
## Nouvelle API
providers.nouvelleapi.enabled = true
providers.nouvelleapi.api.url = "https://api.nouvelleapi.com"
providers.nouvelleapi.api.key = "votre-clé-api"
providers.nouvelleapi.api.secret = "votre-secret"
providers.nouvelleapi.api.rate_limit = 100
providers.nouvelleapi.api.timeout = 30s
```

#### 6. Adapter les modèles et les contrôleurs

Vous devrez peut-être adapter les modèles et les contrôleurs existants pour prendre en charge les spécificités de votre nouvelle API. Consultez les implémentations existantes (Twitter, Bluesky) comme références.

#### 7. Ajouter des tests

N'oubliez pas d'ajouter des tests unitaires et d'intégration pour votre nouvelle implémentation dans le répertoire `test/`.

### Bonnes pratiques pour l'extension

- **Respectez l'architecture existante** : Suivez les patterns établis dans les implémentations de Twitter et Bluesky
- **Gérez correctement les erreurs** : Utilisez le type `Either[String, T]` pour propager les erreurs
- **Documentez votre code** : Ajoutez des commentaires et de la documentation
- **Testez votre implémentation** : Assurez-vous que tout fonctionne correctement avant de le déployer

## Utilisation

### Ajout d'un compte

1. Accédez à la page des comptes
2. Ajoutez un compte possédant un accès développeur sur la plateforme souhaitée (Twitter/X ou Bluesky)
3. Entrez les informations d'authentification nécessaires (token, etc.)

### Configuration d'une collecte

1. Accédez à la page des collectes
2. Créez une nouvelle collecte en spécifiant un nom et le compte à utiliser
3. Définissez les règles de collecte (filtres pour les messages)
4. Activez les règles en les sélectionnant et en cliquant sur "<<"
5. Cliquez sur "Affect rules" pour appliquer les règles
6. Démarrez la collecte en cliquant sur "Start collect"

### Utilisation des modules

1. Accédez à la page de la collecte
2. Cliquez sur "Modules"
3. Configurez le module PostgreSQL (hôte, port, base de données, etc.)
4. Démarrez le module en cliquant sur "Start module"

## Vérification de l'activité

Pour vérifier l'activité de collecte, vous pouvez exécuter cette commande SQL :

```bash
docker exec -it garuda-postgres-1 sh -c "psql -U garuda -d garuda <<EOF
SELECT DATE_TRUNC('month',created_at::timestamp), COUNT(1) AS count
FROM garuda.tweet
GROUP BY DATE_TRUNC('month', created_at::timestamp)
order by DATE_TRUNC('month',created_at::timestamp) desc
limit 10;
EOF"
```

## Nettoyage et désinstallation

Pour arrêter et supprimer les conteneurs Docker :

```bash
# Dans le répertoire docker
cd docker
docker compose down --remove-orphans --volumes

# Pour supprimer également les images
docker rmi $(docker images --filter=reference='garuda-*' -q)

# Nettoyage complet (incluant les images locales)
docker compose down --remove-orphans --volumes --rmi local
```

Pour supprimer l'application, il suffit de supprimer le répertoire du projet. Si vous utilisez les volumes Docker, ils seront conservés à moins de les supprimer explicitement.

## Résolution de problèmes

- **Problèmes de connexion à l'API** : Vérifiez les tokens d'accès et les identifiants dans le fichier de configuration
- **Erreurs de base de données** : Vérifiez les paramètres de connexion et que PostgreSQL est bien en cours d'exécution
- **Collecte ne démarrant pas** : Vérifiez que les règles ont bien été affectées au compte

## Ressources additionnelles

Pour plus d'informations sur le développement et la personnalisation de Garuda, consultez :
- [DEVELOPMENT.md]() : pour le développement (compilation et exécution depuis les sources)
- [docker/README.md]() : pour la gestion des dépendances Docker
- [PACKAGING.md]() : pour le packaging de l'application (fichiers JAR ou image Docker)

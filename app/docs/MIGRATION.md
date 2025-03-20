# Guide de Migration

## Vue d'ensemble

Ce document décrit la stratégie de migration pour l'intégration de Bluesky et la refactorisation de l'architecture modulaire.

## Étapes de Migration

### 1. Préparation

1. Sauvegarder la base de données existante
2. Créer une branche de développement pour la migration
3. Mettre à jour les dépendances du projet

### 2. Migration de la Base de Données

1. Exécuter les scripts de migration pour créer les nouvelles tables :
   ```sql
   -- Table des messages standardisés
   CREATE TABLE IF NOT EXISTS social_message (
     id TEXT PRIMARY KEY,
     provider_type TEXT NOT NULL,
     content TEXT,
     author_id TEXT,
     created_at TIMESTAMP,
     metadata JSONB
   );
   
   -- Table des posts Bluesky
   CREATE TABLE IF NOT EXISTS bluesky_post (
     id TEXT PRIMARY KEY,
     uri TEXT,
     cid TEXT,
     author TEXT,
     text TEXT,
     reply_count INTEGER,
     repost_count INTEGER,
     like_count INTEGER,
     created_at TIMESTAMP,
     indexed_at TIMESTAMP,
     FOREIGN KEY (id) REFERENCES social_message(id)
   );
   ```

2. Migrer les données Twitter existantes vers le nouveau format :
   ```sql
   INSERT INTO social_message (id, provider_type, content, author_id, created_at, metadata)
   SELECT 
     id,
     'Twitter',
     text,
     user_id,
     published_time,
     jsonb_build_object(
       'source', source,
       'lang', lang
     )
   FROM tweet;
   ```

### 3. Déploiement par Étapes

1. **Phase 1 : Infrastructure**
   - Déployer les nouvelles tables
   - Mettre à jour les dépendances
   - Déployer les nouveaux composants sans les activer

2. **Phase 2 : Migration des Données**
   - Exécuter les scripts de migration des données
   - Vérifier l'intégrité des données migrées
   - Valider la compatibilité avec l'ancien format

3. **Phase 3 : Activation des Nouveaux Composants**
   - Activer le nouveau système de providers
   - Activer le système de modules
   - Activer l'interface utilisateur mise à jour

4. **Phase 4 : Validation**
   - Tester avec des comptes Twitter existants
   - Tester avec de nouveaux comptes Bluesky
   - Vérifier la compatibilité descendante

### 4. Rollback

En cas de problème, voici la procédure de rollback :

1. Désactiver les nouveaux composants
2. Restaurer la base de données depuis la sauvegarde
3. Revenir à la version précédente du code

## Nouveaux Composants

### Providers

- `TwitterProvider` : Gère les connexions Twitter
- `BlueskyProvider` : Gère les connexions Bluesky
- `ProviderManager` : Gère les différents providers

### Modules

- `PostgresModule` : Module d'exportation vers PostgreSQL
- Système de plugins pour les modules

### Interface Utilisateur

- Nouveaux formulaires pour les différents types de comptes
- Sélecteur de provider
- Interface unifiée pour tous les providers

## Tests

### Tests Unitaires

- Tests des providers individuels
- Tests des modules
- Tests des composants UI

### Tests d'Intégration

- Tests de l'interaction entre providers
- Tests de la compatibilité descendante
- Tests des scénarios de migration

## Documentation

### API

- Documentation des nouvelles interfaces
- Guide d'ajout de nouveaux providers
- Guide de création de modules

### Utilisateur

- Guide de migration des comptes existants
- Guide d'utilisation des nouveaux comptes
- FAQ sur les changements

## Support

En cas de problème pendant la migration :

1. Consulter les logs d'application
2. Vérifier l'état de la base de données
3. Contacter l'équipe de support technique

## Planning

1. **Semaine 1** : Préparation et infrastructure
2. **Semaine 2** : Migration des données
3. **Semaine 3** : Tests et validation
4. **Semaine 4** : Déploiement et monitoring 
# AppMusic · Remote

Application Android de diagnostic personnel via Telegram, en Kotlin et Material 3. Configurez votre propre bot, choisissez le compte autorisé et démarrez une session visible depuis le téléphone.

Android 8.0+ · interface française · thèmes clair et sombre · aucun serveur à héberger.

Aperçus de l’interface : [thème clair](docs/dashboard-light.png) · [thème sombre](docs/dashboard-dark.png).

[Résultats de validation](docs/VALIDATION.md) : 42 tests réussis, lint sans erreur et compilations debug / release vérifiées.

## Version 2.0

- Tableau de bord : état réel de connexion, dernière synchronisation, compteur de commandes et journal borné.
- Configuration locale : validation des champs, vérification du bot et guide intégré.
- Jeton chiffré avec AES-GCM et une clé Android Keystore ; exclusion des sauvegardes et transferts.
- Autorisation explicite : compte personnel configuré sur le téléphone, conversation privée, identité de l’expéditeur vérifiée. Aucun appairage au premier message.
- Session lancée manuellement, notification avec action d’arrêt, aucune relance au démarrage ou après un arrêt.
- Reconnexion avec délai progressif, prise en compte de `retry_after`, détection des jetons révoqués et conflits de polling.
- Tests de l’autorisation, du protocole, du transport HTTP, du journal et de l’interface sur API 26 et 34 ; tests instrumentés pour le Keystore.

Le dépôt initial contenait aussi des commandes de collecte de SMS, contacts, fichiers, captures et audio. Elles ont été retirées, ainsi que leurs permissions, les callbacks de sélection non authentifiés et le watchdog. Cette version se limite au diagnostic annoncé ci-dessous.

## Installation et configuration

1. Compilez et installez l’APK, ou ouvrez le dossier `src` dans Android Studio.
2. Dans Telegram, créez **un bot dédié à ce téléphone** auprès de [@BotFather](https://t.me/BotFather), avec `/newbot`.
3. Dans AppMusic, renseignez un nom d’appareil, le jeton du bot et **votre identifiant Telegram numérique personnel**. Ce n’est ni un numéro de téléphone, ni un nom `@utilisateur`, ni un identifiant de groupe. Vous pouvez l’obtenir auprès d’un bot de confiance ou via l’API Telegram.
4. Enregistrez, puis utilisez « Vérifier le bot ». Cette vérification confirme le jeton ; elle ne valide pas l’identifiant de votre compte.
5. Ouvrez une conversation privée avec votre bot et envoyez `/start` pour autoriser la conversation côté Telegram.
6. Démarrez la session dans AppMusic, autorisez les notifications sur Android 13+, attendez l’état connecté puis envoyez `/ping`.

Aucun véritable jeton n’est nécessaire pour compiler ou exécuter les tests. N’inscrivez jamais de jeton dans le code source, les tickets ou les journaux.

**Migration depuis la version initiale :** révoquez le jeton précédemment publié dans le dépôt via BotFather. Le supprimer de la version courante ne le retire pas de l’historique Git. Configurez ensuite un nouveau jeton et votre compte dans l’application ; aucune ancienne association n’est reprise automatiquement.

## Commandes

| Commande | Réponse |
| --- | --- |
| `/start`, `/help` | Aide et état de l’association |
| `/ping` | Test de réponse depuis l’appareil |
| `/info` | Nom configuré, modèle, fabricant, version Android et batterie |
| `/devices` | Appareil associé à ce bot |

Les messages de groupe, les autres expéditeurs, les callbacks, les pièces jointes et les commandes inconnues sont ignorés. Les commandes adressées à un autre bot sont ignorées. Les messages de plus de deux minutes sont ignorés ; à la première association, l’ancien historique du bot est écarté. L’horloge du téléphone doit être correcte.

**Un bot par appareil.** Telegram `getUpdates` distribue une file de messages unique par bot. Il ne fournit pas de diffusion vers plusieurs téléphones. Arrêtez toute autre instance utilisant le même jeton et retirez les webhooks avant de démarrer la session. Une flotte multi-appareils nécessiterait un service central de routage, absent de ce projet.

## Développement

Prérequis : JDK 17 ou 21, Android SDK Platform 34 et Build Tools 34.0.0. Le wrapper utilise Gradle 8.7, avec vérification SHA-256 ; le plugin Android est en version 8.5.2.

```bash
cd src
# Si Android Studio ne l’a pas créé, configurez local.properties :
# sdk.dir=/chemin/vers/android-sdk
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
```

- APK installable de développement : `src/app/build/outputs/apk/debug/app-debug.apk`.
- Version optimisée : `src/app/build/outputs/apk/release/app-release-unsigned.apk`. Configurez votre propre signature pour la distribuer.
- Tests unitaires et UI Robolectric : `src/app/build/reports/tests/testDebugUnitTest/index.html`.
- Rapport lint : `src/app/build/reports/lint-results-debug.html`.
- Aperçus rendus par les tests UI : `src/app/build/reports/previews/`.

Sur un appareil ou émulateur connecté :

```bash
cd src
./gradlew connectedDebugAndroidTest
```

Ces tests instrumentés exercent le véritable Android Keystore. Ils effacent la configuration de l’installation de test ; utilisez un appareil de développement. Le CI compile ces tests et publie l’APK debug et les rapports ; leur exécution demande un appareil connecté.

## Organisation

| Composant | Responsabilité |
| --- | --- |
| `MainActivity` | Configuration, guide, validation et tableau de bord |
| `ConfigurationStore` | Chiffrement et stockage des paramètres, curseur de polling |
| `TelegramService` | Session au premier plan, arrêt et reconnexion |
| `TelegramBot`, `TelegramProtocol` | API JSON, gestion HTTP et parsing |
| `CommandPolicy`, `RetryPolicy` | Autorisation, commandes et politique de reprise testables |
| `SessionState`, `UiLogger` | État réactif et journal en mémoire limité à 100 événements |

## Limites et confidentialité

Telegram reçoit le contenu des réponses de diagnostic et les métadonnées habituelles de son API. Le jeton est protégé au repos, mais reste nécessaire en mémoire pendant une session. L’écran de configuration est protégé contre les captures et l’aperçu des applications récentes. Un téléphone compromis ou déverrouillé n’est pas couvert par cette protection.

Android peut interrompre le service pour gérer ses ressources. La session ne se relance pas automatiquement. Une réponse envoyée juste avant une interruption réseau peut être répétée ; les commandes sont donc uniquement consultatives. Aucun test ne contacte un bot réel : validez une conversation de bout en bout avec votre propre bot avant utilisation.

Voir [SECURITY.md](SECURITY.md) pour le périmètre de sécurité.

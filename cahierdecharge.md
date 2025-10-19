# Cahier des Charges : Développement d'une App Overlay Android pour Gestion Audio Multi-Flux

## 1. Introduction et Objectifs

### 1.1 Contexte
Cette application, nommée provisoirement **AudioOverlay Mix** (ou un nom à finaliser), vise à résoudre un problème courant sur Android : permettre aux utilisateurs de jouer à un jeu mobile tout en écoutant de la musique en arrière-plan, sans que cela n'interrompe ou ne dégrade le chat vocal in-app (ex. : Discord, TeamSpeak ou chat intégré à un jeu comme PUBG Mobile). L'app fonctionnera comme un **overlay** (superposition transparente) sur l'écran, offrant un contrôle rapide des volumes pour les flux audio multiples (musique, sons du jeu, chat vocal) sans quitter l'application en cours.

L'approche overlay évite les limitations du mixage cross-app direct (sandboxing Android), en se basant sur les APIs système pour ajuster les volumes globaux ou par catégorie, tout en détectant les apps actives pour des presets intelligents.

### 1.2 Objectifs Principaux
- Fournir un contrôle en temps réel des volumes (sliders) pour musique (media), notifications/chat (voice), et sons système/jeu (system).
- Minimiser les interruptions audio en demandant un focus audio partagé.
- Offrir une interface minimaliste et non intrusive (overlay flottant).
- Assurer une compatibilité avec Android 8+ (Oreo) pour les overlays, et jusqu'à Android 15+.
- Rendre l'app gratuite avec options freemium (pubs optionnelles, presets payants).

### 1.3 Portée du Projet
- **Inclus** : Développement natif Android , overlay UI, gestion audio via AudioManager, détection d'apps, stockage local de presets.
- **Exclus** : Support iOS, mixage audio low-latency (pas de NDK initial), intégration WebRTC pour chat (simuler via volumes), monétisation avancée (intégrer plus tard).
- **Hypothèses** : L'utilisateur accorde les permissions nécessaires (overlay, usage stats, audio focus). Pas de root requis.

## 2. Acteurs et Utilisateurs Cibles

### 2.1 Profils Utilisateurs
- **Gamer mobile principal** : 18-35 ans, joue à des jeux multijoueurs (ex. : Fortnite, Call of Duty Mobile) avec chat vocal, écoute Spotify/YouTube Music en fond. Besoin : Ajustements rapides sans pause.
- **Utilisateur multitâche** : Écoute podcasts/musique pendant des appels VoIP ou jeux casual. Besoin : Interface simple, presets one-tap.
- **Admin/Développeur** : Pour tests, avec mode debug pour logs audio.

### 2.2 Rôles
- **Utilisateur final** : Accède à l'overlay via bouton flottant ou geste.
- **Système Android** : Fournit APIs pour audio et overlays.

## 3. Fonctionnalités Principales

### 3.1 Fonctionnalités Core
1. **Overlay Flottant** :
   - Bouton flottant (comme Facebook Messenger) pour afficher/masquer l'overlay.
   - Overlay semi-transparent (barre latérale ou coin écran) avec 3 sliders : Volume Musique (Media), Volume Chat/Vocal (Voice/Notification), Volume Jeu/Système (System/Ringer).
   - Positionnable par drag-and-drop ; ancré aux bords.

2. **Gestion Audio** :
   - Utilisation d'**AudioManager** pour ajuster volumes en temps réel (setStreamVolume).
   - Demande de **focus audio mix** (AUDIOFOCUS_GAIN_MIX) pour éviter ducking/pause de la musique.
   - Détection automatique des apps actives (via UsageStatsManager) pour appliquer presets (ex. : Si jeu détecté + Spotify, baisser musique de 20%, booster chat).

3. **Presets Intelligents** :
   - 3 presets par défaut : "Jeu Solo" (musique haute, chat bas), "Multi avec Chat" (chat haute, musique moyenne), "Musique Priorité" (tous équilibrés).
   - Sauvegarde personnalisée (SQLite local) ; one-tap pour appliquer.

4. **Notifications et Alertes** :
   - Toast/Notification pour confirmer changements (ex. : "Volume chat boosté").
   - Mode "Auto-Duck" : Réduire temporairement musique lors de détection de voix (via micro permission, basique).

### 3.2 Fonctionnalités Secondaires
- **Thèmes** : Clair/Sombre auto (suivant système) ; taille overlay ajustable.
- **Historique** : Log des ajustements (5 derniers) pour undo.
- **Intégration** : Shortcuts pour lancer depuis tiroir notifications ; widget home pour presets rapides.

### 3.3 Flux Utilisateur Typique
1. Installation et première ouverture : Tutoriel permissions (overlay, usage stats).
2. Lancement : Bouton flottant apparaît ; clic pour overlay.
3. En jeu : Glisser sliders ; app détecte jeu → suggère preset.
4. Fermeture : Masquer overlay ; app en background.

## 4. Modèle de Données

### 4.1 Base de Données Locale (Room/SQLite)
- **Table Presets** :
  - id (PK, int)
  - name (string, ex. "Jeu avec Chat")
  - vol_music (float, 0-1)
  - vol_chat (float, 0-1)
  - vol_system (float, 0-1)
  - app_target (string, package name, ex. "com.spotify.music")

- **Table Logs** :
  - id (PK, int)
  - timestamp (long)
  - action (string, ex. "volume_changed")
  - values (JSON string, {music:0.8, chat:1.0})

Pas de cloud ; tout local pour privacy.

## 5. Contraintes Techniques

### 5.1 Plateforme et Outils
- **Langage** : Kotlin (préféré pour coroutines async).
- **IDE** : Android Studio (Giraffe+).
- **Dépendances** :
  - Core : androidx.core, androidx.appcompat.
  - Audio : android.media.AudioManager.
  - Overlay : WindowManager pour flags SYSTEM_ALERT_WINDOW.
  - DB : Room (androidx.room).
  - Permissions : Manifest + runtime checks (Oreo+ pour overlay).
  - Autres : Coroutines pour détection apps ; Gson pour JSON logs.
- **Cible** : minSdk 26 (Android 8), compileSdk 34+.
- **Sécurité** : Pas de réseau ; chiffrement DB si presets sensibles (optionnel).

### 5.2 Contraintes Système
- **Permissions Requises** :
  - SYSTEM_ALERT_WINDOW (overlay, runtime via Settings.canDrawOverlays).
  - QUERY_ALL_PACKAGES ou USAGE_STATS (détection apps, via UsageStatsManager ; demande spéciale dans settings).
  - MODIFY_AUDIO_SETTINGS (volumes).
  - RECORD_AUDIO (optionnel pour auto-duck).
- **Limitations Android** : Pas de vrai mixage cross-app ; focus mix ne fonctionne pas toujours avec tous les OEM (ex. : Samsung vs Pixel). Tester sur 5 devices.
- **Accessibilité** : Respecter Material Design 3 ; support TalkBack.

### 5.3 Non-Fonctionnelles
- **Performance** : <50ms latence sliders ; batterie <2% impact.
- **UI/UX** : Minimaliste (Material You) ; animations fluides (Lottie pour icônes audio).
- **Tests** : Unit (JUnit), UI (Espresso), E2E sur émulateur + devices réels.
- **Localisation** : Français/English initial ; extensible.

## 6. Livrables

### 6.1 Artefacts
- Code source GitHub (privé initial).
- APK debug/release.
- Documentation : README (setup, build), API audio expliquée.
- Design : Figma mockups (overlay, onboarding).

### 6.2 Phases de Développement
| Phase | Description | Durée Estimée |
|-------|-------------|---------------
| 1. Setup & Permissions | Projet base, permissions overlay/audio. 
| 2. Overlay UI | Bouton flottant + sliders drag. 
| 3. Gestion Audio | AudioManager integration, focus mix. 
| 4. Détection & Presets | UsageStats + Room DB. 
| 5. Tests & Polish | Bugs, thèmes, notifications. 


**Temps Total Estimé** : 2-3 semaines (solo dev, 4h/jour).

## 7. Checklist de Suivi du Projet

Utilisez ce Markdown pour tracker (cocher les cases dans un fichier .md ou GitHub Issues) :

- [ ] **Phase 1 : Setup**
  - [ ] Créer projet Android Studio (Kotlin, minSdk 26).
  - [ ] Ajouter permissions Manifest (SYSTEM_ALERT_WINDOW, etc.).
  - [ ] Implémenter check runtime permissions (onboarding écran).
  - [ ] Tester sur émulateur (Android 10+).

- [ ] **Phase 2 : Overlay**
  - [ ] Créer service foreground pour bouton flottant (WindowManager).
  - [ ] Designer UI overlay (ConstraintLayout, SeekBar sliders).
  - [ ] Ajouter drag-to-move et ancrage bords.
  - [ ] Tester superposition sur jeu (ex. : installer Clash Royale).

- [ ] **Phase 3 : Audio**
  - [ ] Intégrer AudioManager (getStreamVolume, setStreamVolume).
  - [ ] Demander audio focus mix (AudioFocusRequest).
  - [ ] Lier sliders à volumes (streams : STREAM_MUSIC, STREAM_VOICE_CALL, STREAM_SYSTEM).
  - [ ] Tester avec Spotify + jeu + appel VoIP.

- [ ] **Phase 4 : Avancé**
  - [ ] Implémenter UsageStatsManager pour détection apps.
  - [ ] Créer Room DB pour presets/logs.
  - [ ] Ajouter boutons presets one-tap.
  - [ ] Option auto-duck (basique, avec RECORD_AUDIO).

- [ ] **Phase 5 : Final**
  - [ ] Thèmes Material You ; animations.
  - [ ] Logs undo (5 derniers).
  - [ ] Tests : Unit (volumes), UI (sliders), E2E (scénario jeu+musique).
  - [ ] Debug mode (logs Adb).

- [ ] **Phase 6 : Release**
  - [ ] Générer signed APK.
  - [ ] Rédiger store listing (screenshots overlay en action).
  - [ ] Soumettre Play Console (catégorie Outils > Personnalisation).

## 8. Risques et Mitigations
- **Risque** : Overlay bloqué par OEM (ex. : Xiaomi MIUI). **Mitigation** : Guide utilisateur pour activer ; fallback sans détection apps.
- **Risque** : Changements Android 15 (privacy audio). **Mitigation** : Suivre AOSP updates ; version modulaire.
- **Budget** : Gratuit (open-source option) ; outils free.

Ce cahier des charges est complet mais itératif – ajustez-le au fur et à mesure. Si vous voulez un prototype code (ex. : snippet overlay), ou focus sur une phase, dites-moi ! Bonne chance pour le dev, ça va cartonner pour les gamers. 🚀
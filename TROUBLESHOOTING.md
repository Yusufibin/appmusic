# Guide de Dépannage - AudioOverlay Mix

## 🔴 L'application se ferme immédiatement au démarrage

### Causes possibles

1. **Permissions manquantes**
   - L'application nécessite plusieurs permissions critiques pour fonctionner
   - Si une permission est refusée, l'application peut se fermer

2. **Crash du service**
   - Le service d'overlay peut crasher lors de son démarrage
   - Cela peut être dû à des ressources manquantes ou des erreurs de configuration

3. **Problèmes de compatibilité Android**
   - Certaines versions d'Android peuvent avoir des restrictions supplémentaires

---

## ✅ Solutions

### 1. Vérifier les permissions dans les paramètres Android

Allez dans **Paramètres → Applications → AudioOverlay Mix → Permissions** et vérifiez que les permissions suivantes sont accordées :

- ✓ **Microphone** (RECORD_AUDIO) - Obligatoire
- ✓ **Afficher par-dessus d'autres apps** (SYSTEM_ALERT_WINDOW) - Obligatoire
- ✓ **Accès aux statistiques d'utilisation** (PACKAGE_USAGE_STATS) - Obligatoire
- ✓ **Notifications** (POST_NOTIFICATIONS) - Recommandé (Android 13+)

### 2. Accorder l'accès aux statistiques d'utilisation

Cette permission spéciale doit être accordée manuellement :

1. Allez dans **Paramètres → Applications → Accès spécial → Accès aux données d'utilisation**
2. Trouvez **AudioOverlay Mix** dans la liste
3. Activez l'option

### 3. Vérifier les logs système (pour développeurs)

Pour diagnostiquer le problème exact, utilisez `adb logcat` :

```bash
# Filtrer uniquement les erreurs de l'application
adb logcat | grep -i "com.company.product"

# Ou voir tous les crashs
adb logcat | grep -i "AndroidRuntime"
```

### 4. Réinstaller l'application

Si le problème persiste :

1. Désinstallez complètement l'application
2. Redémarrez votre appareil
3. Réinstallez l'application
4. Accordez toutes les permissions dès le premier lancement

### 5. Vérifier l'espace de stockage

L'application utilise une base de données Room. Assurez-vous d'avoir :
- Au moins **100 MB d'espace libre** sur votre appareil

### 6. Désactiver les optimisations de batterie

Certains fabricants (Xiaomi, Huawei, Samsung, etc.) ont des gestionnaires de batterie agressifs :

1. Allez dans **Paramètres → Batterie → Optimisation de batterie**
2. Trouvez **AudioOverlay Mix**
3. Sélectionnez **Ne pas optimiser**

---

## 🐛 Problèmes connus et solutions

### Le service ne démarre pas

**Symptôme** : L'application se ferme sans message d'erreur

**Solution** :
- Vérifiez que toutes les permissions sont accordées
- Redémarrez votre appareil
- Vérifiez les logs avec `adb logcat`

### Crash sur Android 13+

**Symptôme** : Crash immédiat sur Android 13 ou supérieur

**Solution** :
- Accordez la permission de **POST_NOTIFICATIONS**
- Cette permission est obligatoire pour les services de premier plan sur Android 13+

### Crash sur Xiaomi/MIUI

**Symptôme** : L'application ne peut pas afficher l'overlay

**Solution** :
1. Allez dans **Paramètres → Applications → Gérer les applications → AudioOverlay Mix**
2. Activez **Afficher fenêtres contextuelles**
3. Activez **Afficher au premier plan**
4. Désactivez l'optimisation de batterie

### Crash sur Huawei/EMUI

**Symptôme** : Le service est tué immédiatement

**Solution** :
1. Allez dans **Paramètres → Batterie → Lancement d'application**
2. Trouvez **AudioOverlay Mix**
3. Désactivez la gestion automatique
4. Activez manuellement tous les types de lancement

---

## 📊 Collecter des informations de débogage

Si vous devez signaler un bug, collectez ces informations :

### 1. Version Android
```bash
adb shell getprop ro.build.version.release
```

### 2. Modèle de l'appareil
```bash
adb shell getprop ro.product.model
```

### 3. Logs de crash
```bash
adb logcat -d > logcat.txt
```

### 4. Informations de l'application
```bash
adb shell dumpsys package com.company.product
```

---

## 🔧 Mode de débogage

Pour activer les logs détaillés pendant le développement :

1. Ouvrez l'application
2. Si elle ne crash pas immédiatement, le bouton flottant apparaîtra
3. Tapez 5 fois sur le bouton pour activer le mode debug
4. Les logs détaillés seront affichés dans logcat

---

## 📱 Compatibilité

### Versions Android supportées
- **Minimum** : Android 8.0 (API 26)
- **Cible** : Android 14 (API 34)

### Testé sur
- ✓ Android 13 (Pixel, Samsung)
- ✓ Android 12 (Xiaomi, OnePlus)
- ✓ Android 11 (Samsung, Huawei)
- ⚠️ Android 8-10 (support basique)

---

## 💡 Conseils supplémentaires

### Pour un fonctionnement optimal :

1. **Accordez toutes les permissions** dès le premier lancement
2. **Désactivez l'optimisation de batterie** pour l'application
3. **Ajoutez l'app aux applications protégées** (sur Xiaomi/Huawei)
4. **Gardez l'application à jour** avec les dernières corrections de bugs

### Si l'application fonctionne mais l'overlay ne s'affiche pas :

1. Vérifiez la permission d'affichage par-dessus d'autres apps
2. Redémarrez le service via les paramètres système
3. Vérifiez qu'aucun bloqueur d'overlays n'est actif

---

## 📞 Obtenir de l'aide

Si les solutions ci-dessus ne résolvent pas votre problème :

1. Vérifiez les [Issues GitHub](https://github.com/votre-repo/issues)
2. Créez une nouvelle issue avec :
   - Modèle de votre appareil
   - Version Android
   - Logs de crash (logcat)
   - Étapes pour reproduire le problème

---

## 🔄 Changelog des corrections

### v1.0.1 (À venir)
- ✅ Ajout de la gestion de la permission RECORD_AUDIO à l'exécution
- ✅ Ajout de la gestion de la permission POST_NOTIFICATIONS (Android 13+)
- ✅ Amélioration de la gestion d'erreurs au démarrage du service
- ✅ Ajout de messages de statut plus clairs
- ✅ Délai avant fermeture de MainActivity pour assurer le démarrage du service

### v1.0.0
- Version initiale
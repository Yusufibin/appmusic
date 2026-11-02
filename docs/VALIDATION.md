# Validation du 2 novembre 2026

Commande exécutée depuis une compilation propre :

```bash
cd src
./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
```

Résultat : **BUILD SUCCESSFUL**. Outillage utilisé : JDK 17, Gradle 8.7, Android Gradle Plugin 8.5.2, SDK 34.

- **42 tests réussis**, aucune erreur ni aucun échec : autorisation, fraîcheur des commandes, protocole, transport HTTP factice, reprise, journal et interface.
- Interface exercée avec Robolectric sur **API 26 et 34** ; rendus natifs clair et sombre contrôlés visuellement. Les aperçus utilisent des champs de démonstration, sans jeton.
- **Lint : aucune erreur**. Les sept avertissements restants signalent des versions plus récentes des dépendances ; les versions actuellement utilisées sont celles de cette compilation vérifiée.
- APK debug généré et vérifié avec `apksigner verify`.
- APK release généré avec réduction du code et des ressources ; il est **non signé**.
- APK des tests instrumentés compilé. Les tests du véritable Android Keystore ne sont pas exécutés ici, faute de cible Android opérationnelle.
- Les permissions de l’APK ont été contrôlées : Internet, notifications et service au premier plan de synchronisation, plus la permission interne générée par AndroidX. Aucun accès aux données personnelles ou capteurs.

Les appels réseau des tests utilisent un transport factice et des jetons synthétiques. Aucune conversation avec un bot réel n’a été testée. La validation sur téléphone avec un bot personnel reste à effectuer suivant `SECURITY.md`.

Branche de travail : `improve/secure-remote-diagnostics`.

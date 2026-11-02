# Sécurité

AppMusic 2.0 permet uniquement un diagnostic consultatif d’un téléphone dont l’utilisateur a démarré la session localement.

## Frontières d’accès

- Le compte autorisé est enregistré localement avant la session. Aucun message distant ne peut changer ce compte.
- Chaque commande vérifie le type privé de la conversation, son identifiant, l’identité de l’expéditeur, l’absence de bot et la fraîcheur du message.
- Les callbacks et fichiers ne déclenchent aucune action. Les permissions SMS, contacts, stockage, microphone et projection d’écran ont été retirées.
- Le service n’est pas exporté, n’écoute aucun port entrant, ne redémarre ni au boot ni via une alarme et utilise `START_NOT_STICKY`.
- Le transport utilise HTTPS, refuse les redirections et ferme les connexions en fin de requête ou à l’arrêt de la session.
- Les erreurs réseau affichées ne contiennent pas l’URL de requête. Le journal ne contient ni le texte reçu ni les identifiants Telegram ; une règle masque aussi les jetons reconnaissables.
- Le jeton et l’identifiant du compte sont chiffrés ensemble via AES-GCM. La clé est conservée dans Android Keystore ; l’application exclut ses préférences des sauvegardes et des transferts.

## Ancien jeton publié

La version initiale contenait un jeton de bot directement dans le dépôt. Son propriétaire doit le révoquer auprès de BotFather. Ne réutilisez pas ce jeton et ne le reproduisez pas dans un rapport. Cette refonte ne purge pas l’historique du dépôt distant.

## Validation manuelle avant utilisation

Avec un bot de test dédié et un téléphone de développement :

1. Vérifier `/ping` et `/info` depuis le compte configuré.
2. Vérifier qu’un autre compte et un groupe ne reçoivent aucune réponse.
3. Couper puis rétablir Internet : l’interface doit afficher la reprise puis la reconnexion.
4. Arrêter via l’écran et via la notification : aucune nouvelle commande ne doit être traitée.
5. Redémarrer le téléphone : le service doit rester arrêté.
6. Vérifier la restauration des paramètres après fermeture de l’application, puis leur suppression avec « Oublier la configuration ».

Les tests unitaires ne prouvent ni l’état d’un bot réel ni la protection d’un appareil compromis. Pour signaler un problème, fournissez la version Android, les étapes et un journal expurgé, sans jeton ni données personnelles.

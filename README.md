# AXON

Application Android native réalisée en Kotlin et Jetpack Compose. Interface et réglages en français.

## Construire l'APK debug

Prérequis : JDK 17, Android SDK avec la plateforme Android 36 et les Build Tools. Si aucun Gradle n'est installé :

```bash
gradle wrapper --gradle-version 8.13
./gradlew assembleDebug
```

APK produit : `app/build/outputs/apk/debug/app-debug.apk`.

## Ce jalon

- Écran cockpit sombre avec noyau animé et navigation vers le chat et les réglages.
- Trois apparences sélectionnables et conservées sur l'appareil : Cyber (par défaut), Minuit et Clair.
- Historique de conversation conservé localement entre les redémarrages.
- L'interface signale explicitement que les réponses IA ne sont pas encore activées : aucun fournisseur IA n'est configuré et les messages ne sont pas prétendus comme traités.

Le chat persistant, les thèmes et l'interface sont réellement utilisables. Les fournisseurs IA, le mode vocal Live, Linux/PRoot, les outils/MCP, les agents et la mémoire avancée ne sont pas encore implémentés dans ce jalon. L'application ne nécessite ni clé API ni accès réseau.

Cet APK est une version **debug** signée avec la clé de débogage : elle convient à l'installation et aux essais, pas à une publication Play Store. La signature de publication devra être créée séparément.

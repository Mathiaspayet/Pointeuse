# MaPointeuse 📱

Application Android moderne de gestion de pointage et de suivi du temps de travail.

<div align="center">

  ![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
  ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
  ![Material Design](https://img.shields.io/badge/Material%20Design%203-757575?style=for-the-badge&logo=material-design&logoColor=white)

</div>

## ✨ Fonctionnalités

- ⏱️ **Chronomètre en temps réel** - Suivi précis de votre temps de travail
- ⏸️ **Gestion des pauses** - Démarrez et arrêtez vos pauses facilement
- 📊 **Statistiques détaillées** - Vue par jour, semaine, mois et année
- 📜 **Historique complet** - Consultez tous vos pointages passés
- 📍 **Géolocalisation** - Enregistrement automatique de votre position
- 🎯 **Détection automatique** - Geofencing GPS pour démarrer/arrêter automatiquement au lieu de travail
- 🔔 **Notifications intelligentes** - Notifications avec actions rapides à l'arrivée/départ du travail
- 🎨 **Interface moderne** - Design Material Design 3 épuré et intuitif
- ♿ **Accessible** - Support complet des lecteurs d'écran

## 📸 Captures d'écran

_[Ajoutez vos captures d'écran ici]_

## 🛠️ Technologies utilisées

- **Langage** : Kotlin
- **UI** : Jetpack Compose
- **Architecture** : MVVM (Model-View-ViewModel)
- **Base de données** : Room Database
- **Navigation** : Jetpack Navigation Compose
- **Permissions** : Activity Result API
- **Services** : Foreground Service pour le suivi en arrière-plan
- **Material Design** : Material 3 (Material You)

## 📋 Prérequis

- Android Studio Hedgehog ou plus récent
- Android SDK 35
- Kotlin 1.9+
- JDK 17

## 🚀 Installation

1. **Clonez le dépôt**
   ```bash
   git clone https://github.com/Mathiaspayet/Pointeuse.git
   cd Pointeuse
   ```

2. **Ouvrez le projet dans Android Studio**
   - Ouvrez Android Studio
   - Sélectionnez "Open an existing project"
   - Naviguez vers le dossier du projet

3. **Synchronisez les dépendances Gradle**
   - Android Studio devrait automatiquement synchroniser
   - Sinon, cliquez sur "Sync Now"

4. **Compilez et exécutez**
   - Connectez un appareil Android ou lancez un émulateur
   - Cliquez sur le bouton "Run" (▶️)

## 🏗️ Structure du projet

```
app/src/main/java/com/mapointeuse/
├── data/                      # Couche de données
│   ├── AppDatabase.kt         # Configuration Room Database
│   ├── Pointage.kt            # Entité Pointage
│   ├── Pause.kt               # Entité Pause
│   ├── WorkPlace.kt           # Entité Lieu de travail
│   ├── PointageDao.kt         # Data Access Object
│   ├── WorkPlaceDao.kt        # DAO pour lieux de travail
│   ├── PointageRepository.kt  # Repository pattern
│   └── Converters.kt          # Type converters Room
├── ui/                        # Interface utilisateur
│   ├── PointageViewModel.kt   # ViewModel principal
│   ├── StatistiquesViewModel.kt
│   ├── StatistiquesScreen.kt
│   ├── HistoriqueViewModel.kt
│   ├── HistoriqueScreen.kt
│   ├── EditPointageViewModel.kt
│   ├── EditPointageScreen.kt
│   ├── ParametresViewModel.kt # ViewModel paramètres
│   ├── ParametresScreen.kt    # Écran de configuration
│   └── theme/                 # Thème Material Design
├── service/                   # Services en arrière-plan
│   ├── PointageService.kt     # Service de suivi
│   ├── GeofencingManager.kt   # Gestion du geofencing
│   └── NotificationHelper.kt  # Gestion des notifications
├── utils/                     # Utilitaires
│   └── PermissionHelper.kt    # Gestion des permissions
└── MainActivity.kt            # Activité principale
```

## 🎯 Utilisation

### Démarrer une journée de travail

1. Ouvrez l'application
2. Appuyez sur **"Commencer la journée"**
3. Le chronomètre démarre automatiquement

### Gérer les pauses

- Appuyez sur **"Pause"** pour mettre en pause votre temps de travail
- Appuyez sur **"Reprendre"** pour continuer

### Terminer la journée

- Appuyez sur **"Terminer la journée"** pour arrêter le pointage
- Vos données sont automatiquement sauvegardées

### Consulter les statistiques

- Naviguez vers l'onglet **"Statistiques"**
- Sélectionnez la période (Jour/Semaine/Mois/Année)
- Consultez votre temps total et moyenne

### Configurer la détection automatique (Geofencing)

1. Naviguez vers l'onglet **"Paramètres"**
2. Renseignez le nom de votre lieu de travail
3. Entrez les coordonnées GPS ou appuyez sur **"Utiliser ma position actuelle"**
4. Ajustez le rayon de détection (par défaut 100m)
5. Activez les options souhaitées :
   - **Notification à l'arrivée** : Recevez une notification avec action rapide
   - **Notification au départ** : Recevez une notification avec action rapide
   - **Démarrage automatique** ⚠️ (expérimental) : Lance le pointage automatiquement
   - **Arrêt automatique** ⚠️ (expérimental) : Termine le pointage automatiquement
6. Appuyez sur **"Enregistrer"**

Une fois configuré, l'application détectera automatiquement quand vous arrivez ou quittez votre lieu de travail et vous proposera de commencer/terminer votre journée.

## 📱 Permissions requises

- **Localisation** : Pour enregistrer votre position lors des pointages
- **Localisation en arrière-plan** : Pour le suivi continu et le geofencing
- **Notifications** : Pour afficher les notifications de suivi et de geofencing

## 🧪 Tester le Geofencing (Émulateur)

Pour tester la détection automatique GPS sur un émulateur Android :

1. **Configurez un lieu de travail** dans l'onglet Paramètres
2. **Démarrez un pointage** pour activer le service GPS
3. **Simulez des déplacements GPS** via ADB :

```bash
# Position initiale (au bureau)
adb -s emulator-5554 emu geo fix -122.084 37.421998

# Sortir de la zone (>100m)
adb -s emulator-5554 emu geo fix -122.082 37.421998

# Retourner au bureau
adb -s emulator-5554 emu geo fix -122.084 37.421998
```

4. **Vérifiez les logs** pour voir la détection :
```bash
adb -s emulator-5554 logcat | grep "GeofencingManager"
```

**Résultats attendus** :
- Notification "Arrivée au bureau" avec bouton "Commencer"
- Notification "Départ du bureau" avec bouton "Terminer"
- Anti-spam : 5 minutes minimum entre notifications

## 🔄 Versions

### Version 1.1 (Actuelle)

- ✅ Interface utilisateur moderne et épurée
- ✅ Système de pointage complet
- ✅ Gestion des pauses
- ✅ Statistiques détaillées
- ✅ Historique complet
- ✅ Support de la géolocalisation
- ✅ **Détection automatique par GPS (Geofencing)**
- ✅ **Notifications intelligentes avec actions rapides**
- ✅ **Configuration du lieu de travail**
- ✅ Notifications en temps réel
- ✅ Accessibilité complète

## 🤝 Contribution

Les contributions sont les bienvenues ! Pour contribuer :

1. Forkez le projet
2. Créez une branche pour votre feature (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Poussez vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## 📝 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 👤 Auteur

**Mathias Payet**

- GitHub: [@Mathiaspayet](https://github.com/Mathiaspayet)

## 🙏 Remerciements

- [Material Design](https://m3.material.io/) pour les guidelines de design
- [Jetpack Compose](https://developer.android.com/jetpack/compose) pour le framework UI
- [Room](https://developer.android.com/training/data-storage/room) pour la base de données

---

<div align="center">
  Fait avec ❤️ et Kotlin
</div>

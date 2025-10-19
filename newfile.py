# -*- coding: utf-8 -*-

from PIL import Image
from pathlib import Path
import shutil

# ==============================================================================
# CONFIGURATION
# Modifiez simplement les valeurs ci-dessous.
# ==============================================================================

# 1. Chemin vers votre image pour l'icône de l'application.
#    (Ex: "C:/MesImages/logo.png" ou simplement "logo.png" si dans le même dossier)
LOGO_SOURCE_PATH = "l.png"

# 2. Chemin vers votre image pour l'écran de chargement (splash screen).
#    Ici, nous utilisons la même que le logo.
SPLASH_SOURCE_PATH = "ll.png"

# 3. Chemin vers la racine de votre projet Android.
#    Le script trouvera automatiquement le dossier '.../app/src/main/res'.
ANDROID_PROJECT_PATH = "C:/Chemin/Vers/Votre/ProjetAndroid"

# ==============================================================================
# DÉFINITIONS DES ICÔNES (généralement pas besoin de modifier)
# ==============================================================================

# Tailles standard pour les icônes Android en pixels
ICON_SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192,
}

# Noms de fichiers standards pour les icônes
SQUARE_ICON_NAME = 'ic_launcher.png'
ROUND_ICON_NAME = 'ic_launcher_round.png'
FOREGROUND_ICON_NAME = 'ic_launcher_foreground.png'
SPLASH_SCREEN_NAME = 'logo_splash.png'


def generate_android_icons(source_path: Path, res_path: Path):
    """
    Génère les icônes de l'application Android pour toutes les densités
    à partir d'une seule image source.
    """
    print("\n--- Début de la génération des icônes de l'application ---")
    try:
        with Image.open(source_path).convert("RGBA") as base_image:
            # Boucler sur chaque densité (mdpi, hdpi, etc.)
            for density, size in ICON_SIZES.items():
                dir_name = f'mipmap-{density}'
                target_dir = res_path / dir_name
                target_dir.mkdir(parents=True, exist_ok=True)
                
                print(f"  -> Traitement pour {density} ({size}x{size}px)")

                # Redimensionner l'image avec un antialiasing de haute qualité
                resized_image = base_image.resize((size, size), Image.Resampling.LANCZOS)

                # Enregistrer l'icône carrée et ronde
                resized_image.save(target_dir / SQUARE_ICON_NAME, 'PNG')
                resized_image.save(target_dir / ROUND_ICON_NAME, 'PNG')

            print("  -> Icônes carrées et rondes générées avec succès.")

            # Générer l'icône de premier plan pour les icônes adaptatives (plus grande)
            drawable_dir = res_path / 'drawable'
            drawable_dir.mkdir(exist_ok=True)
            foreground_size = 432  # Taille recommandée pour une bonne qualité (108dp * 4)
            foreground_image = base_image.resize((foreground_size, foreground_size), Image.Resampling.LANCZOS)
            foreground_path = drawable_dir / FOREGROUND_ICON_NAME
            foreground_image.save(foreground_path, 'PNG')
            print(f"  -> Icône de premier plan (adaptative) enregistrée dans : {foreground_path}")

    except FileNotFoundError:
        print(f"ERREUR : Le fichier source '{source_path}' n'a pas été trouvé.")
        return False
    except Exception as e:
        print(f"ERREUR : Une erreur est survenue lors du traitement de l'image : {e}")
        return False
        
    print("--- Génération des icônes terminée ---")
    return True


def generate_splash_screen(source_path: Path, res_path: Path):
    """
    Copie l'image source dans le dossier drawable pour servir de splash screen.
    """
    print("\n--- Début de la génération du Splash Screen ---")
    try:
        target_dir = res_path / 'drawable'
        target_dir.mkdir(parents=True, exist_ok=True)
        
        destination_path = target_dir / SPLASH_SCREEN_NAME
        
        # Copie le fichier source vers la destination
        shutil.copy2(source_path, destination_path)
        
        print(f"  -> Logo pour le splash screen copié dans : {destination_path}")

    except FileNotFoundError:
        print(f"ERREUR : Le fichier source '{source_path}' n'a pas été trouvé.")
        return False
    except Exception as e:
        print(f"ERREUR : Une erreur est survenue lors de la copie du fichier : {e}")
        return False
        
    print("--- Génération du Splash Screen terminée ---")
    return True


def main():
    """
    Fonction principale du script.
    """
    print("=============================================")
    print("= Lancement du générateur d'assets Android  =")
    print("=============================================")

    # Convertir les chemins en objets Path pour une meilleure manipulation
    logo_path = Path(LOGO_SOURCE_PATH)
    splash_path = Path(SPLASH_SOURCE_PATH)
    project_root = Path(ANDROID_PROJECT_PATH)
    
    # Valider les chemins
    res_directory = project_root / 'app' / 'src' / 'main' / 'res'

    if not project_root.is_dir():
        print(f"\nERREUR CRITIQUE: Le dossier du projet '{project_root}' n'existe pas.")
        return

    if not res_directory.is_dir():
        print(f"\nERREUR CRITIQUE: Le dossier 'res' n'a pas été trouvé.")
        print(f"Vérifié ici : {res_directory}")
        print("Assurez-vous que le chemin du projet Android est correct.")
        return

    # Lancer les générations
    generate_android_icons(logo_path, res_directory)
    generate_splash_screen(splash_path, res_directory)

    print("\nOpération terminée avec succès !")


if __name__ == "__main__":
    main()
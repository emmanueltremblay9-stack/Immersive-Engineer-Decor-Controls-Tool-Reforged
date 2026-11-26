# Installer les trois interfaces Factory Study V3

Ce guide concerne le **JAR candidat fourni avec cette livraison** : Factory Hopper, Factory Dropper et Small Lab Furnace. Le fichier livré s'appelle `IEDCTR-Factory-Study-V3-MC1.21.1-NeoForge.jar` ; sa version interne reste `1.1.67-reconstructed`. Son **SHA-256**, indiqué dans le rapport de livraison, identifie précisément les octets qualifiés. Consulter `README.fr.md` pour les résultats de la campagne et ses limites avant de choisir ce fichier.

## Versions

Les dépendances déclarées viennent de `src/main/resources/META-INF/neoforge.mods.toml`. Les versions fixées pour construire la campagne viennent de `gradle.properties` et de la chaîne Java 21 du build.

| Élément | Exigence déclarée ou de construction | Version fixée pour reproduire la cible de qualification |
|---|---|---|
| Minecraft Java Edition | Exactement **1.21.1** | 1.21.1 |
| Java | **Java 21** pour la chaîne de construction | Temurin 21.0.13.1 |
| NeoForge | **21.1.230 ou supérieur**, dans le mod | 21.1.230 pour Minecraft 1.21.1 |
| Immersive Engineering | Obligatoire, **12.4.2-194 ou supérieur** | `1.21.1-12.4.2-194` |
| JEI | Facultatif ; **19.32.0.359 ou supérieur** lorsqu'il est présent | 19.32.0.359 |
| IEDCTR | JAR de cette livraison | `IEDCTR-Factory-Study-V3-MC1.21.1-NeoForge.jar`, identifié par son SHA-256 ; version interne `1.1.67-reconstructed` |

La colonne des versions fixées décrit la cible exacte de la campagne. Une plage déclarée autorisant des versions ultérieures ne prouve pas que toutes ces versions ont été testées. Le rapport de livraison distingue compilation, essais Minecraft exécutés et contrôles restant manuels.

## Installation dans une instance d'essai

1. **Fermer Minecraft.** Dupliquer l'instance dans le lanceur, ou copier son dossier dans un emplacement d'essai. Conserver une sauvegarde du dossier `mods`, des configurations et des mondes que l'on souhaite garder. Un premier essai dans un monde neuf permet de vérifier les trois machines sans modifier un monde habituel.
2. Vérifier que cette instance utilise **Minecraft 1.21.1, NeoForge et Java 21**, avec Immersive Engineering présent dans `mods`. Pour reproduire les résultats de la livraison, employer les versions fixées dans le tableau. JEI reste facultatif.
3. Dans le dossier `mods` de cette instance, **déplacer l'ancienne version d'IEDCTR vers la sauvegarde, hors de `mods`**. Garder les autres mods nécessaires. Il doit rester **une seule version du mod** `immersive_engineer_decor_controls_tool_reforged` ; deux fichiers portant ce même identifiant constituent un doublon, même si leurs noms diffèrent.
4. Copier le JAR de cette livraison dans `mods`. Les trois textures et le code d'interface sont intégrés au JAR : aucune copie séparée des PNG, des sources Java ou du module de validation n'est nécessaire.
5. Comparer le SHA-256 du fichier copié avec celui du rapport. Sous PowerShell :

   ```powershell
   Get-FileHash ".\mods\IEDCTR-Factory-Study-V3-MC1.21.1-NeoForge.jar" -Algorithm SHA256
   ```

   Sous Linux :

   ```bash
   sha256sum mods/IEDCTR-Factory-Study-V3-MC1.21.1-NeoForge.jar
   ```

   Exécuter la commande depuis le dossier de l'instance. Si le nom du JAR livré a été renommé, utiliser son nom réel ; la somme doit rester identique.

6. Lancer l'instance et ouvrir un monde d'essai. Pour ce premier contrôle, utiliser les ressources du mod sans pack remplaçant ses interfaces. Vérifier dans la liste des mods que Minecraft a chargé IEDCTR et ses dépendances.

## Contrôle manuel des trois blocs

Placer les trois machines depuis l'inventaire créatif du monde d'essai, puis ouvrir chacune par l'interaction habituelle.

| Machine | Ce qui doit être visible | Essai utile |
|---|---|---|
| **Factory Hopper / Entonnoir d'usine** | 18 cases machine, inventaire 27 + 9, trois réglages et trois commandes | Régler portée, délai et quantité ; changer les modes redstone/déclenchement ; vérifier un transfert vers un inventaire raccordé selon l'orientation de la machine. L'impulsion manuelle demande une action ; le transfert dépend des conditions de la machine. |
| **Factory Dropper / Largueur d'usine** | 12 cases de réserve, 3 vraies cases de filtre, inventaire 27 + 9 et huit commandes | Mettre quelques objets dans la réserve ; régler vitesse, direction, quantité et délai ; essayer le signal manuel. Tester ensuite les filtres avec leurs quantités. Les objets des filtres sont réellement stockés et leur nombre fait partie de la condition de correspondance. |
| **Small Lab Furnace / Petit fourneau de laboratoire** | 11 cases machine, inventaire 27 + 9, flamme, progression et énergie séparées | Cuire un minerai avec du combustible et contrôler le résultat. La jauge FE reste distincte du combustible. Un réchauffeur de fourneau Immersive Engineering dans un emplacement auxiliaire peut accélérer la cuisson lorsqu'il est alimenté ; le combustible reste nécessaire. |

Pour les trois interfaces, prendre puis reposer une pile, essayer le clic droit, le transfert avec Maj et la répartition par glisser. Vérifier les quantités et les infobulles près des bords. Une pile portée bloque les commandes de machine et l'interface explique comment les utiliser à nouveau.

Essayer les échelles d'interface **1 et 2**, puis l'échelle habituelle de l'instance ; redimensionner aussi la fenêtre. Les images, les objets et les zones de clic doivent rester alignés. Avec les commandes Hopper/Dropper, vérifier Tab, les flèches, Entrée/Espace et Début/Fin ; sur le pavé de direction, Début recentre les deux axes.

Avec JEI présent, cliquer la flèche de progression du Lab sans pile portée pour ouvrir les recettes de cuisson, puis vérifier les raccourcis recettes/usages sur un objet. Sans JEI, le Lab conserve ses fonctions de cuisson et n'affiche pas cette indication de raccourci.

## Retour à la version précédente

Fermer Minecraft, déplacer le nouveau JAR hors de `mods`, puis remettre **l'unique ancien JAR** conservé dans la sauvegarde. Reprendre l'instance ou le monde sauvegardé si l'on souhaite retrouver exactement l'état précédent. Conserver les mêmes dépendances que cette ancienne instance.

La livraison fournit un candidat local et ses preuves identifiées. La procédure ci-dessus est une installation manuelle ; elle ne constitue pas un déploiement déjà réalisé, une release publique ou une admission au Vault. La couverture multijoueur sur serveur dédié et les autres limites doivent être lues dans le rapport, séparément des essais en monde local.

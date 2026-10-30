# SMALL MINERAL SMELTER HYBRID — Mécaniques in-game

**Page brouillon v0.4 · 30 octobre 2026 · Minecraft Java 1.21.1 / NeoForge · DIRTY STUDYO / IEDCTR**  
**État : PROPOSED / DOCUMENT DE CONCEPTION — ni implémenté, ni approuvé globalement, ni testé en jeu.**  
**Responsable de l'implémentation et des acceptations : owner IEDCTR.**  
**Portée : un seul bloc, `small_mineral_smelter` ; PR documentaire uniquement, sans modification du code, des sauvegardes ni du gameplay.**

## 1. Vision et boucle joueur

Le *Small Mineral Smelter* est une petite machine industrielle isolée qui chauffe progressivement des matériaux minéraux jusqu'au **Magma**, puis éventuellement jusqu'à leur **liquéfaction en lave**. La GUI peut sélectionner la destination du traitement : **Lava**, **Magma Block** ou **Obsidian**. Le produit sélectionné détermine à quel stade le traitement aboutit et quels composants sont consommés. La machine conserve un **réservoir de lave réel** et une batterie FE ; les sorties d'objets et de fluides sont automatisables.

Boucle joueur : installer et orienter → raccorder l'énergie à l'arrière → fournir des minéraux ou de la lave par le haut → choisir le produit dans la GUI → observer la progression thermique et les contraintes → récupérer les objets / fluides par le bas ou dans la GUI → laisser refroidir avant manipulation. **La machine chaude est dangereuse au contact.**

## 2. Portée des choix et références

**CIBLE EXPRIMÉE PAR LE PROPRIÉTAIRE** : progression davantage fidèle aux minéraux vers lave, architecture hybride ; **BACK = FE input**, **TOP = item/fluid input**, **BOTTOM = item/fluid output** ; Magma Block et Obsidian sélectionnables dans la GUI ; danger pour le joueur lorsqu'elle travaille à chaud ; tank interne visible.

**ORIGINAL CONFIRMÉ — ED 1.3.29, Minecraft 1.19.2/Forge** : phases thermiques Cold / Hot / Magma / Lava ; progressivité en température ; produit Magma intermédiaire puis lave ; refroidissement lorsque l'énergie cesse ou que la redstone bloque la chauffe ; sortie de lave par dessous ; deux slots internes limités à un objet ; réservoir de **2 000 mB**, batterie **32 000 RF** ; comparateur par phase **0/5/10/15** ; interaction chaude susceptible d'enflammer le joueur. Référence code `EdMineralSmelter.java`, manuel Patchouli et tag `accepted_mineral_smelter_input` au commit `a561e61e35ce0367088356967e1ea4cad5c1f27d`.

**IEDCTR ACTUEL CONFIRMÉ — `main@bb50370fa86590a5e3159b6053b8cb6af4c7d64b`** : 180 ticks à 16 FE/t pour **1 Magma Block + 250 mB lava** par minéral ; 180 ticks de refroidissement pour **1 000 mB lava → 1 Obsidian** ; 16 000 FE ; réservoir 4 000 mB ; redstone HIGH bascule vers le refroidissement ; trois cases affichées mais la case centrale « Heat » refuse les items ; entrée minérale en slot 0 et sortie item en slot 2. Ces valeurs ne sont pas automatiquement celles du modèle hybride.

**PRINCIPE** : restaurer le sens de la transformation thermique originale ; conserver les extensions convenues ; ne pas réintroduire ses bugs connus et ne pas casser les sauvegardes sans migration validée.

## 3. Orientation et ports (contrat cible)

| Face mondiale relative | Interface physique | Sens et règles |
|---|---|---|
| **FRONT** | Face de service / interaction | Ouvrir la GUI, lire état et danger ; aucune entrée/sortie automatique par défaut. |
| **BACK = opposite(FACING)** | Port **FE input** | `EnergyStorage.BLOCK` en réception **uniquement** ; ne produit / n'extrait pas de FE vers l'extérieur. |
| **TOP = UP** | Port **item + lava input** | `ItemHandler.BLOCK` : insertion dans **l'unique slot minéral** ; `FluidHandler.BLOCK` : **remplissage en lave seulement** du réservoir. Les deux capacités sont indépendantes. |
| **BOTTOM = DOWN** | Port **item + lava output** | `ItemHandler.BLOCK` : extraction du **seul slot produit** ; `FluidHandler.BLOCK` : **vidange** du réservoir. Aucune insertion par le bas. |
| **LEFT / RIGHT** | Aucune automatisation par défaut | Pas de fuite latérale de capacités ou de permissions. |

Un bloc adjacent placé **sous** le Smelter doit exposer les capacités nécessaires, ou utiliser un bloc de routage multi-capacités. Un simple coffre ne peut pas absorber des fluides et un tuyau fluide ne peut pas absorber des items. Un raccordement FE par câble IE requiert un **connecteur/capability adapter compatible**, ce n'est pas une prise directe de fil garantie.

**Politique de sortie proposée (à confirmer)** : exposition passive de capacités extractibles **et** tentative de transfert automatique vers le récepteur du dessous ; transfert partiel simulé/confirmé, borné par tick, sans duplication. Si le dessous n'accepte pas, l'item ou la lave reste stocké. Pas d'éjection au sol automatique sans choix explicite. Les débits et l'ordre des transferts restent à spécifier.

## 4. Inventaires et composants internes

- **Input minéral : 1 slot**, entrée via GUI ou face supérieure ; uniquement items figurant dans un **tag extensible** de minéraux admissibles. Éviter une liste Java fermée ; préserver notamment pierres, cobblestone/deepslate, granit/diorite/andésite et autres familles du tag ED original après validation 1.21.1. Le type d'item et ses composants sont conservés jusqu'à consommation du lot.
- **Output produit : 1 slot**, extraction GUI + face inférieure ; Magma Block ou Obsidian selon mode. Aucun joueur/tuyau ne peut y insérer des objets. La capacité de stack respecte le maximum de l'item vanilla ; élargissement par rapport au slot original limité à 1 = extension **PROPOSED**.
- **Aucun troisième slot « Heat » inventé** : la chaleur est une **valeur/état thermique**, pas un combustible mis dans un slot. Le slot index 1 de la version actuelle est réservé à l'évolution/migration ; son contenu existant éventuel doit être protégé.
- **Tank interne** : lave uniquement, entièrement visible dans la GUI ; **4 000 mB** comme capacité de continuité avec IEDCTR actuel (**ASSUMED**, à approuver). Le cap historique est 2 000 mB. Le réservoir doit conserver les mB exacts, le type de fluide et les composants compatibles.
- **Batterie interne** : 16 000 FE comme point de départ de continuité (**ASSUMED**, à approuver), apport depuis l'arrière ; facturation réelle côté serveur, pas d'énergie créée par les changements de mode.

Le Smelter peut recevoir directement de la lave par le haut même sans minéral, **uniquement pour la stocker ou l'utiliser dans un mode de solidification compatible**. L'eau, le lait et les fluides inconnus sont refusés dans ce contrat.

## 5. Modèle thermique persistant

Température logique **`thermal_progress` = 0 à 100** ; ce n'est **pas** un relevé en °C. Quatre phases à préserver conceptuellement :

| Phase | Plage candidate inspirée de l'original | Comparateur | Conséquence |
|---|---|---|---|
| **COLD** | 0–4 | 0 | Aucun risque thermique ; prêt à chauffer. |
| **HOT** | 5–89 | 5 | Chauffe active ou refroidissement ; surface dangereuse. |
| **MAGMA** | 90–99 | 10 | Produit intermédiaire prêt si mode Magma ; surface dangereuse. |
| **LAVA** | 100 | 15 | Liquéfaction possible vers tank ; surface dangereuse. |

Les seuils sont ceux du code original ; la cinétique exacte et la façon d'exposer un indicateur de température lors du refroidissement restent à approuver. **Valeur d'étude** : chauffe d'environ **2 points/s** et refroidissement d'environ **1 point/s**, fidèles au paramétrage historique, en distinguant vitesse, dépense réelle FE et cadence serveur. Ce ne sont pas des réglages IEDCTR adoptés.

Le chauffage réclame un minéral admissible, de la place pour le produit prévu, assez de FE pour l'étape et une entrée redstone qui n'inhibe pas. Lorsque les conditions cessent, la machine ne doit pas instantanément effacer une température élevée : elle refroidit selon une règle déterministe, sans avancer gratuitement la production.

## 6. Sélecteur de produit dans la GUI

**Trois modes proposés pour rendre l'intention du joueur explicite** :

### LAVA — liquéfaction

1. Un minéral admis est présent ; vérifier qu'un rendement complet de lave peut être stocké **avant d'engager irréversiblement le lot**.
2. Chauffer progressivement **COLD → HOT → MAGMA → LAVA** en payant l'énergie effectivement consommée.
3. À la liquéfaction, consommer **1 unité du minéral** et créditer le tank de **1 000 mB de lave** (**rendement proposé**, par analogie historique, à confirmer).
4. Aucun Magma Block gratuit n'est créé lors de ce chemin. La sortie item reste inchangée ; la lave peut être extraite en bas.

### MAGMA BLOCK — arrêt à la phase intermédiaire

1. Avec un minéral accepté, chauffer jusqu'à l'étape **MAGMA**.
2. Si la sortie item peut accepter **1 Magma Block**, consommer exactement **1 minéral**, créditer exactement **1 Magma Block** et ne pas créer de lave pour ce lot.
3. Le cœur peut ensuite refroidir ; le mode choisi reste enregistré. Si le tank contient déjà de la lave, il **ne se vide pas automatiquement** du seul fait de ce mode. Une conversion facultative « 1 000 mB lava → Magma Block par refroidissement » devra faire l'objet d'une **décision séparée**.

### OBSIDIAN — solidification de lave

1. Si le réservoir possède au moins **1 000 mB de lave**, enclencher une phase de refroidissement contrôlé vers **Obsidian** sans demander de minéral supplémentaire.
2. Sinon, si un minéral est disponible et si la place dans le tank le permet, passer par la liquéfaction d'un lot de minéral puis refroidir les **1 000 mB** ainsi obtenus.
3. À la fin du refroidissement et si le slot de sortie peut accepter le produit, débiter **1 000 mB** puis créditer **1 Obsidian** en une transaction serveur protégée.
4. Pas de consommation de lave si la sortie est bloquée. Pas d'Obsidian si la quantité réelle est insuffisante. La vitesse/dépense du refroidissement restent à valider.

**Changement de mode en cours de traitement** : le clic demande un **mode en attente** ; le serveur applique le mode au **prochain lot non engagé**, pas au lot déjà entamé. La GUI distingue « sélection demandée », « sélection confirmée » et « mode actif sur le lot courant ». Une conversion d'un lot partiellement chauffé ou d'un réservoir déjà chaud ne doit jamais produire deux récompenses.

## 7. Redstone et contrôle de l'opération

- **Référence originale à préserver : HIGH = inhibition du chauffage / favorise le refroidissement ; LOW = chauffe permise**, sous réserve de FE et de ressources.
- Le **mode de production** ne change pas la direction du signal redstone et n'est pas un bouton ON/OFF électrique.
- Sans FE, la machine ne chauffe pas ; son cœur peut refroidir. **Pas de débit FE facturé pour un tick non effectué**.
- Une entrée redstone HIGH ne doit pas produire de blocs d'obsidienne en secret quand le mode et les préconditions ne l'autorisent pas. L'état thermique reste suivi.
- Le comparateur doit rapporter la **phase thermique 0/5/10/15**, indépendamment de la sélection « Lava/Magma/Obsidian » ; les transitions et la lecture d'un tank chaud sans lot doivent être définies de façon cohérente.
- Les modes « Ignore / Invert » présents dans la discussion du **Block Breaker** ne sont **pas** importés automatiquement dans le Smelter. Une configuration redstone supplémentaire exige une décision spécifique.

## 8. FE, bilan matière et blocages

Le modèle FE reste **à équilibrer et à approuver** : actuellement IEDCTR calcule 16 FE/t × 180 ticks pour le cycle de Magma ; l'original vise une chauffe plus longue et une batterie de 32 000 RF. Ne pas inventer de **consommation variable par dureté** (c'était un sujet du Block Breaker, pas de ce Smelter).

Conditions **avant destruction / conversion de ressources** :
- entrée valide et lot enregistré ; place dans la sortie adéquate ; capacité réservoir suffisante ; FE disponibles pour l'étape ; données de cible stables ; aucune requête serveur périmée.
- quand le tank est plein, **LAVA attend** sans écraser du fluide ou consommer indéfiniment des minéraux ; **MAGMA** peut fonctionner si sa sortie est disponible et que son contrat le permet ; **OBSIDIAN** peut débloquer le tank en consommant réellement 1 000 mB.
- si la sortie item est pleine, le lot attend une extraction ; jamais de production cachée dans un troisième slot.
- consommation FE **uniquement pour du travail payé** ; aucun rendement artificiel en alternant redstone, GUI, chunk reload ou connecteurs.

## 9. Risque thermique pour les joueurs

**Cible exprimée : le bloc chaud est dangereux pendant son fonctionnement.** Contrat proposé : lorsqu'une entité touche réellement la surface du Smelter en phase **HOT / MAGMA / LAVA** pendant une opération, appliquer des dégâts de type thermique côté serveur, avec cadence bornée et respect des immunités (Creative/Spectator, résistance au feu, immunité feu appropriée). **Pas d'aura de dégâts à distance**, pas de dommage parce qu'on ouvre simplement l'écran depuis un autre endroit.

**Proposition à approuver** : démarrer l'équilibrage à **½ cœur par seconde** de contact continu ; contrôler les dégâts par entité plutôt que par appel de tick pour éviter les cumuls abusifs. En refroidissement, maintenir l'avertissement **HOT SURFACE** tant que le cœur reste réellement au-dessus du seuil de danger ; décider séparément si les dégâts persistent à l'arrêt. L'ancien ED pouvait appliquer une brève ignition lors d'une interaction à main vide ; restaurer exactement cet effet est facultatif et distinct du dommage au contact.

Avertissement GUI et indication visuelle/in-world : **surface chaude** avec icône et message textuel, couleurs secondaires non exclusives. Les effets sonores/particules sont liés à la phase et leur coût/performance doivent être vérifiés.

## 10. Panneau GUI conforme au gameplay

Réutiliser le **langage métallique Engineer's Decor**, **pas** le style des GUIs Immersive Engineering ; IE sert seulement de référence comportementale FE. La GUI contient :

1. **Thermal Process** : phase Cold / Hot / Magma / Lava, progression thermique réelle, direction chauffe/refroidissement, état danger, progression du lot actif et raison d'attente.
2. **Material & Product** : 1 slot minéral + 1 slot produit ; sélecteur de résultat **Lava / Magma / Obsidian** ; sélection active/attente clairement identifiées.
3. **Internal Lava Tank** : jauge intégrée avec quantité exacte, **mB réels**, type lave, capacité, blocage plein ou disponibilité pour solidification.
4. **FE & Redstone** : énergie stockée/capacité, consommation actuelle réelle (pas consommation souhaitée), signal HIGH/LOW reçu et effet sur la chauffe, état de la sortie.
5. **Inventory** : inventaire joueur **3 × 9 = 27 slots**, plus **1 × 9 = 9 slots hotbar**. Les interactions de slot restent gérées par le menu serveur.

La GUI ne doit **pas** afficher une température physique en °C, une action de combustible fictive ou un mode dont l'implémentation serveur n'existe pas. États `SYNCING`, `WORKING`, `HEATING`, `COOLING`, `WAIT_FE`, `REDSTONE_INHIBITED`, `TANK_FULL`, `OUTPUT_FULL`, `NO_INPUT`, `INVALID_INPUT`, `READY_TO_COLLECT`, `HOT_SURFACE` à associer à des **données serveur réelles** ; les états ne sont pas exclusifs (tank plein + surface chaude peuvent coexister).

## 11. Persistance, sécurité et rétrocompatibilité

- Enregistrer : mode sélectionné, mode appliqué au lot courant, progress thermique, état de lot, objet entrant, FE, quantité de lave, état de refroidissement, sortie et état/versions de synchronisation. À relecture, invalider proprement les progressions hors bornes sans dupliquer ni détruire un lot valide.
- Préserver le stockage des mondes existants : slots `0` (entrée), `2` (sortie), ancien `1` (« Heat »), `Energy`, `FluidAmount`, `FluidType`, `Progress`, `MineralCooling`. Fournir un **plan explicite de migration** avant toute modification de schéma ; ne pas déplacer/effacer des items silencieusement.
- Sorties/fill/drain et création d'item : serveur autoritaire ; simulation de place, transaction atomique ou reprise déterministe après crash, refus des réponses périmées. Aucune duplication de lave/obsidienne/magma via extraction simultanée et changement de mode.
- **Démontage du bloc chaud ou contenant de la lave : règle NON DÉCIDÉE.** Protéger les items et la lave ; choisir explicitement si le fluide est conservé dans l'item, transféré en conteneur, déversé en jeu sous conditions ou signalé comme perdu. Pas de destruction de monde ni vidange involontaire sans décision et sauvegarde testée.
- Les conditions « un conteneur inférieur accepte deux capacités » et « les pipes / câbles chargent correctement » doivent être testées en solo, serveur dédié, au redémarrage et sur chunk unload/reload.

## 12. Tests d'acceptation à exécuter par l'owner DEV

| Précondition | Action | Attendu vérifiable |
|---|---|---|
| Input minéral admissible, FE, tank libre, mode Lava | Laisser progresser jusqu'à liquéfaction | Un minéral retiré ; rendement de lave exact ; pas de Magma Block gratuit ; comparateur 0→5→10→15. |
| Input admissible, FE, mode Magma | Chauffer jusqu'à Magma | Un Magma Block créé ; pas de lave ajoutée ; extraction GUI et bottom correcte. |
| ≥ 1 000 mB lave, mode Obsidian, sortie libre | Refroidir jusqu'à fin | Exactement 1 000 mB débités et un Obsidian créé, aucun minéral inventé. |
| Tank proche capacité, mode Lava | Lancer un lot | Aucune perte de lave ni consommation irréversible si 1 000 mB ne rentrent pas. |
| Sortie item pleine | Finir Magma ou Obsidian | Résultat mis en attente, pas de perte, doublon ou FE facturés pour un travail impossible. |
| Port FE branché à l'arrière uniquement | Alimenter puis tenter FE sur autres faces | FE accepté au dos ; rejet ailleurs ; aucun FE extrait par automation. |
| Ports du haut et du bas | Insérer item/lave en haut, extraire item/lave en bas | Deux capacités distinctes fonctionnent sans contournement ni mélange ; pas d'entrée inversée. |
| Signal redstone HIGH | Alimenter machine en chauffe | Plus de chauffe ; refroidissement contrôlé ; pas d'Obsidian hors contrat ; comparateur cohérent. |
| FE insuffisants en phase HOT | Interrompre puis rétablir l'alimentation | Aucun travail gratuit ; refroidissement/reprise selon contrat validé. |
| Joueur sans résistance au contact d'une surface chaude | Rester au contact puis se retirer | Dégâts au rythme choisi, pas d'aura, arrêt immédiat à distance ; exemptions respectées. |
| Sauvegarde ancien IEDCTR avec items, FE et lave | Mettre à niveau et rouvrir | Aucune donnée perdue ; schéma migré ou migration bloquée en sécurité. |
| Transfert simultané item + fluide, redémarrage/chunk | Extractions et reprise | Conservation de toutes les quantités ; pas de duplication ni transfert par la mauvaise face. |
| GUI tailles/langues variées | Ouvrir, cliquer, lire | 27+9 slots vanilla, contrôles lisibles, alignement correct, messages non trompeurs. |

**Statut des tests : NOT_PERFORMED.** Les GameTests actuels du dépôt vérifient l'ancienne mécanique cyclique IEDCTR et doivent être **réconciliés**, pas présentés comme validations du nouveau modèle.

## 13. Décisions restantes avant handoff de code

| ID proposé | Question | Proposition / état |
|---|---|---|
| `SMS-HYB-D01` | Capacité de tank | 4 000 mB pour continuité IEDCTR, **PROPOSED** ; original 2 000 mB. |
| `SMS-HYB-D02` | Batterie et FE/t, vitesse thermique | 16 000 FE comme continuité ; courbe et cadence **UNKNOWN**. |
| `SMS-HYB-D03` | Rendement par minéral | 1 000 mB pour Lava ; 1 Magma Block pour Magma, **PROPOSED**, tag d'entrée à valider. |
| `SMS-HYB-D04` | Mode Obsidian | Consomme 1 000 mB → 1 Obsidian ; cycle de refroidissement requis, **PROPOSED**. |
| `SMS-HYB-D05` | Magma à partir de lave déjà stockée | **UNCERTAIN** ; ne pas implémenter automatiquement. |
| `SMS-HYB-D06` | Transferts bas | Passive + éventuel push actif borné, débit/priorité **UNKNOWN**. |
| `SMS-HYB-D07` | Contact chaud | Seuil HOT+, cadence et résistance, **PROPOSED** ; dégâts en cours de refroidissement à trancher. |
| `SMS-HYB-D08` | Redstone et comparator | Préférence phases originales 0/5/10/15, sans import des modes du Breaker. |
| `SMS-HYB-D09` | Changement de mode en cours | Appliqué au prochain lot après confirmation serveur, **PROPOSED**. |
| `SMS-HYB-D10` | Migration et démontage du tank | **BLOCKED pour implémentation** tant que sort du fluide/inventaire non défini. |

**Aucun « next », aperçu de GUI, note ou version `FINAL` n'approuve implicitement ces mécanismes.** Le présent document est un brouillon autonome à soumettre à l'owner IEDCTR pour examen. **PREPARED ≠ SENT ≠ RECEIVED ≠ EXECUTED.**

## 14. Sources de vérité et limites

- Original historique : https://github.com/stfwi/engineers-decor/blob/a561e61e35ce0367088356967e1ea4cad5c1f27d/src/main/java/wile/engineersdecor/blocks/EdMineralSmelter.java
- Manuel historique : https://github.com/stfwi/engineers-decor/blob/a561e61e35ce0367088356967e1ea4cad5c1f27d/src/main/resources/data/engineersdecor/patchouli_books/engineersdecor_manual/en_us/entries/crafting/small_mineral_smelter.json
- Tag historique : https://github.com/stfwi/engineers-decor/blob/a561e61e35ce0367088356967e1ea4cad5c1f27d/src/main/resources/data/engineersdecor/tags/items/accepted_mineral_smelter_input.json
- IEDCTR actuel : https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/blob/bb50370fa86590a5e3159b6053b8cb6af4c7d64b/src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java
- GameTests actuels : https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/blob/bb50370fa86590a5e3159b6053b8cb6af4c7d64b/src/main/java/com/oblixorprime/engineersdecorreforged/gametest/MachineGuiGameTests.java

**Évidence** : lecture des sources à références épinglées, comparaison statique et conception. **Non vérifié** : capacité mécanique réellement approuvée par l'owner, fonction des nouveaux ports en jeu, rendu et intégration GUI, sauvegardes migrées, damage tests, CI et release. **Soumis pour revue par PR documentaire ; aucune écriture de code/Notion ni intégration gameplay.**

# Factory Study V3 — intégration des trois interfaces

Ce dossier documente les interfaces Factory Hopper, Factory Dropper et Small Lab Furnace intégrées dans IEDCTR. Les images sont les PNG RGBA autorisés à la publication, conservés octet pour octet. Leur identité et leurs chemins runtime figurent dans provenance.json.

## Inventaire

- Factory Hopper : 54 positions visibles, dont 18 cases machine, 27 cases d’inventaire et 9 cases de barre rapide.
- Factory Dropper : 51 positions visibles, dont 15 cases machine, 27 cases d’inventaire et 9 cases de barre rapide.
- Small Lab Furnace : 47 positions visibles, dont 11 cases machine, 27 cases d’inventaire et 9 cases de barre rapide.

Les ancrages d’objets et de commandes sont décrits dans les trois fichiers de correspondance. Les contrôles de sélection, survol et clic utilisent la même géométrie de référence que le rendu. JEI utilise l’écran Factory Study pour le raccourci de recettes du four.

## Comportement conservé

Les indices de cases, inventaires, commandes existantes, recettes et règles d’automatisation restent ceux du mod. L’écran Small Lab Furnace n’ajoute aucune règle de chauffe : le combustible demeure nécessaire selon le backend existant, et les réchauffeurs auxiliaires gardent leur effet déjà implémenté. Le scope ne modifie pas MachineBlockEntity.

## Vérification

La compilation et les tests Java sont exécutés par les workflows de la PR. Le workflow Factory Study V3 Native démarre le client Minecraft avec le JAR normal empaqueté, ouvre les trois écrans, couvre les 152 positions visibles, les commandes et les raccourcis JEI, puis vérifie les deux langues à froid. Chaque preuve est rattachée au commit et au SHA du JAR réellement chargé. Les essais multijoueur, lecteur d’écran et acceptation artistique par le propriétaire demeurent distincts.

Ce contenu décrit un candidat en PR brouillon. Il ne constitue pas une release et ne demande aucune installation dans une instance personnelle.

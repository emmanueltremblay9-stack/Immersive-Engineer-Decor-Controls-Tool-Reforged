# Compiler et qualifier Factory Study V3

Ce protocole vise le commit exact de la PR brouillon #206. Il n’utilise pas les preuves du paquet source comme résultats de cette révision.

## Prérequis

- JDK complet Java 21.
- Git, Python 3 et les dépendances Gradle déjà déclarées dans le dépôt.
- Pour le client automatisé : Linux/X11, Xvfb, XTest, Mesa OpenGL logiciel et une surface virtuelle de 1920 × 1080.

## Contrôle local

Depuis une copie propre du commit candidat :

    java -version
    javac -version
    bash ./gradlew clean check jar --stacktrace

Le JAR normal attendu se trouve sous build/libs. Vérifier son identifiant NeoForge et comparer les trois entrées PNG du JAR aux empreintes indiquées dans provenance.json. Le source set de qualification ne doit pas être présent dans le JAR normal.

## Contrôle Minecraft de la PR

Le workflow Factory Study V3 Native vérifie le SHA de la tête de PR, produit le JAR normal, puis l’ouvre avec un serveur intégré dans deux démarrages à froid : anglais et français. Il emploie l’interaction normale des blocs, couvre les trois menus, leurs 152 positions, les états de commande et les raccourcis JEI. Le rapport lie le JAR empaqueté, l’empreinte des sources et le commit candidat.

Les captures Minecraft et les rapports de chaque exécution résident dans le répertoire temporaire du runner. Ils ne sont pas ajoutés aux sources du mod. Une capture n’est publiable dans une discussion de PR que si elle provient de cette tête exacte et ne révèle aucune donnée privée.

Le test à serveur intégré ne démontre pas le comportement en multijoueur dédié, l’usage prolongé d’une même session, l’écoute d’un lecteur d’écran ni l’approbation visuelle finale du propriétaire.

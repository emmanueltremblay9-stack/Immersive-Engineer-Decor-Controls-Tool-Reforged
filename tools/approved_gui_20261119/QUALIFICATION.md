# Owner 19 novembre : candidat public-minimal v3 (LOCAL seulement)

Aucun commit/push/PR/publication. Aucun JAR ou rendu natif qualifié.
Les deux fonds PNG livrés précédemment restent inchangés.

## Séparation des preuves

Le dépôt proposé ne contient ni les deux références originales privées, ni les
224 copies historiques. Leurs noms/hashes et l'inventaire historique sont des
métadonnées, pas leurs octets.

La baseline est extraite dans un dossier temporaire hors dépôt depuis l'objet
Git exact `87f8b8e03cac46a6154b54522d1497baa52c67b2`, avec vérification des
224 hashes et des chemins. Les assertions historiques restent inchangées.
Si l'objet manque, validation locale refusée sans réseau. Dans GitHub CI
uniquement (`CI=true` et `GITHUB_ACTIONS=true`), le gate acquiert uniquement
cet objet : `git fetch --no-tags --depth=1 origin <SHA exact>`. Aucun checkout
ni déplacement de branche ; aucune écriture distante. Ce protocole est testé
avec doubles, pas via un nouveau run GitHub.

Les trois modifications du renderer sont inversées exactement (hash candidat,
plages ancrées, hash de restauration). Inventaires et hashes des guards/client/
assets sont fermés ; Python -O est refusé. La baseline extraite est revérifiée
sur chaque passage. Les anciens tests d'art prouvent uniquement les pixels
historiques, pas les nouvelles images.

La CI publique contrôle les deux identités sources **attestées**, les octets
exacts des cinq assets de sortie, le générateur versionné, les limites/slots,
les variations visibles des puits, la projection rayée attestée et les tests
négatifs. Elle ne peut PAS reconstruire les pixels depuis des originaux qu'elle
n'a pas. Il s'agit d'une séparation explicite, pas d'une nouvelle preuve de
provenance pixel directe en CI.

Cette reconstruction a été exécutée deux fois dans le paquet privé v2 immuable :
SHA256 `e910eda70ae7c5fd2ac74fdf90e310b03ae47b1a1a725e9cbf3186ba464ff64e`.
Pour la reproduire localement : définir `IEDCTR_OWNER_SOURCE_DIR` vers un dossier
externe au checkout contenant les deux originaux SHA-pinnés, puis lancer
`python tools/approved_gui_20261119/build_assets.py` (Pillow requis seulement
pour ce générateur, pas pour les tests publics). Le générateur refuse un dossier
source sous la racine du dépôt.

## Commandes CI

```
python -S -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_factory*tooltip*.py' -v
python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_factory*.py' -v
python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_*.py' -v
```

## Limites natives

Le préflight est le glob du workflow existant. Sa réussite n'est pas celle du
client. Le script natif exige Xvfb/xvfb-run/xauth, puis un checkout propre commité,
puis construit et vérifie le JAR avant prepareFactoryControlTooltipClientRun et
runFactoryControlTooltipClient. Le delta local non commité et l'environnement
Xvfb bloqué ne permettent pas d'affirmer que runClient a été atteint.
Clic pile tenue, états/glyphes, clipping et rendu Minecraft restent NOT RUN.

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

## v4 local follow-up to the rejected ef40 runtime (not published)

The original v3 recipe remains byte-identical historical evidence. The active
v4 recipe is a compact, closed six-path candidate delta and two exact Java
inverses, not an alternate acceptance path. The historical 87f8 inventory and
all its guard hashes remain unchanged. ef40's old Hopper and Dropper PNGs are
explicitly rejected as the new candidate. No private original PNG or historical
fixture copy is added to this repository.

v4 changes only empty-well tonal depth, removes the generic rim overdrawing
registered borders, and corrects the Hopper ribbon using an isotropically sampled
central painted strip, separate rails and three 5px source heads. Selection is
still drawn after the background. Slot coordinates, input boundaries, widgets,
state symbols and Dropper ribbon remain unchanged. The RGB curve may shift chroma
slightly; it is restricted to empty cavity pixels, not a whole-panel adjustment.

The previous private v2 reproduction archive recorded in v3 remains historical;
it does not prove v4 reproduction. v4 reproduction uses the same two external
SHA-pinned originals and the newly pinned generator. Public CI checks the exact
outputs, source attestations, unchanged pixel regions, nearest-sampled cavity
contrast, baseline inverses, and rejection tests. It never reconstructs unavailable
private source images. The old ef40 object may be fetched by exact SHA only in
GitHub CI for previous-candidate rejection and pixel-delta tests; local tests fail
closed if historical objects are unavailable.

No v4 native capture, actual held-stack click, or owner artistic acceptance is
claimed. The held-stack scenario is separately prepared and must not reuse the
old (-8,83) outside witness: that point is now inside the visible protected panel;
the correct outside witness is (-9,83). No remote run or publication is authorized
by this documentation.

## v5 Metal Assembly: local preparation, not native-tested

Owner superseding art direction: all visible internal backgrounds are assembled
metal plates; slot backgrounds are matte steel; the central yellow/black ribbon
becomes a metal plate bar. The exact November19 images still control geometry and
identity, but their black/hazard fills are no longer the desired background art.

The compact v5 recipe replaces exactly three PNGs and pins the new generator and
five mandatory metal regression tests. v3 and v4 manifests are unmodified. Old
R04, ef40 and 8f90 assets fail as current candidates. Java, layout, slot coordinates,
input boundaries, native glyph atlas and control thumbs are unchanged. Fixed
control background `owner_control_well.png` changes only dark neutral opaque pixels;
its luminous/copper border pixels and alpha remain exact.

The unmodified v4 art tests continue to run in a separately constructed temporary
view of exact8f90 Git bytes. This is historical regression evidence, NOT evidence
for new metal art. The native workflow's existing preflight also runs the v5 gate
and all five new metal tests on actual candidate files. No sources/fixtures are
copied into the public repository. CI may fetch only the three specifically pinned
Git commits87f8,ef40,8f90; local missing objects fail without a network request.

Public preflight uses standard-library PNG decoding, no Pillow/numpy dependency.
Private reproduction requires external SHA-pinned originals and Pillow:
`IEDCTR_OWNER_SOURCE_DIR=<external-originals> python tools/approved_gui_20261119/build_metal_assets.py`.

Metal checks cover all105 matte opaque textured item footprints, unchanged slot
rims/exterior/alpha and protected bright control pixels, no central hazard stripe,
unchanged production/semantic-control assets, and the darker runtime fixed-well
background. Reproduction and1x/2x asset inspection do not establish a Minecraft
render, actual item legibility, cursor visibility or owner approval. Held-stack
work remains in a distinct local test-only copy and is NOT part of this candidate.

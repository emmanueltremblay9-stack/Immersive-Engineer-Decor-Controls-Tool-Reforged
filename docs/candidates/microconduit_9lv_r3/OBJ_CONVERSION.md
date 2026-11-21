# OBJ conversion from R3 editable sources

Each OBJ was generated from the unmodified source BBModel (source paths in docs/candidates/microconduit_9lv_r3/source), preserving mesh faces, UVs and cuboids. All model coordinates are in 0..1 block units; UVs are normalized and vertically flipped to OBJ's V convention. No runtime geometry opening / native texture proof has been executed.

Original source files are the authoritative editable files. To reproduce: read the corresponding .bbmodel JSON, add mesh vertices to their element origins, write each face with per-vertex UVs, and triangulate or quadrangulate cube faces. Use the same R3 PNG atlas for all three, with NeoForge loader 'neoforge:obj' and '#texture0' in the MTL.

[
  {
    "name": "microconduit_c01",
    "faces": 2408,
    "vertices": 9568,
    "meshes": 49,
    "cubes": 42,
    "bytes": 579412,
    "model_hash": "6b5b8c73baa117aeb1fdffbd7ed5f0d5c4f3f4a3",
    "obj_hash": "950bc440a6663a79e51268094e464a9fa6f35759",
    "mtl_hash": "221f1fa9c638881d87b79978bc7c985abaa8031e"
  },
  {
    "name": "machine_connector_c03",
    "faces": 616,
    "vertices": 2204,
    "meshes": 16,
    "cubes": 8,
    "bytes": 132021,
    "model_hash": "5bd8958fb40d09792c421a0d4cf9c145a610050e",
    "obj_hash": "41f34dbf612c680ba12e0d0f1646ace667e7e579",
    "mtl_hash": "221f1fa9c638881d87b79978bc7c985abaa8031e"
  },
  {
    "name": "structural_support_s01",
    "faces": 972,
    "vertices": 3756,
    "meshes": 15,
    "cubes": 28,
    "bytes": 226542,
    "model_hash": "9df09f7fe1ad5481be06068580244da61347dacc",
    "obj_hash": "40a90120c0e359bba60f5fcaa9e01233520cb661",
    "mtl_hash": "221f1fa9c638881d87b79978bc7c985abaa8031e"
  }
]

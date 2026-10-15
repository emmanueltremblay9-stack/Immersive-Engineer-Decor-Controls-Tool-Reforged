# Water canal visual investigation

User screenshots show a brick canal terminating at steel catwalk stairs, with visible upstream water and gray exposed treads. Both supplied images were inspected. This change adds evidence collection; it does not yet establish a production defect or claim a fix.

The existing nonce-guarded native client runner creates its own fresh integrated world. After the existing lamp and fluidlogging regressions, it captures oblique and overhead views of four scenes: waterlogged stairs, dry stairs (empty canal so adjacent water cannot fill the control), waterlogged low catwalk, and waterlogged raised catwalk. Brick sides, floor, upstream sources and an open top form a bounded synthetic canal. Creative flight holds the camera still; each teleport must synchronize before waiting for rendered frames. The input isolation screen renders no background or blur.

The proof records client block/fluid states and direct fluid tessellation. The gate requires upper fluid quads in wet cases and none in the dry control, plus matching screenshot names, PNG headers and dimensions. These checks do not prove final pixel visibility. Review the eight PNGs in the `water-canal-scenes` CI artifact. Only one stair orientation is currently covered; this is not an exact reconstruction of the user's world or shader/resource-pack environment.

No installed JAR, Prism instance or user world is read or modified. Passive accumulator behavior and all production block/fluid logic remain unchanged. No release or installation is part of this investigation.

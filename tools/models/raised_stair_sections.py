"""Exact pole-section contours and a continuous loft into the approved V.

The approved surface at y >= -4 is unchanged. Only the straight stem below it
is replaced. Section boundaries come from the union of the actual pole cuboids.
"""
import math

PROFILES = {
    'wood': 'straight_pole_model',
    'thin_steel': 'straight_thin_metal_pole_model',
    'thick_steel': 'straight_thick_metal_pole_model',
}
TOP = [(5.5,5.75),(5.75,5.5),(10.25,5.5),(10.5,5.75),
       (10.5,10.25),(10.25,10.5),(5.75,10.5),(5.5,10.25)]

def contours(elements):
    rects = [(e['from'][0], e['from'][1], e['to'][0], e['to'][1]) for e in elements]
    xs=sorted({x for r in rects for x in (r[0],r[2])})
    zs=sorted({z for r in rects for z in (r[1],r[3])})
    cells=[];edges=set()
    for x0,x1 in zip(xs,xs[1:]):
        for z0,z1 in zip(zs,zs[1:]):
            if not any(a <= (x0+x1)/2 <= c and b <= (z0+z1)/2 <= d for a,b,c,d in rects):continue
            cells.append((x0,z0,x1,z1))
            points=[(x0,z0),(x1,z0),(x1,z1),(x0,z1)]
            for a,b in zip(points,points[1:]+points[:1]):
                if (b,a) in edges:edges.remove((b,a))
                else:edges.add((a,b))
    loops=[]
    while edges:
        a,b=min(edges);loop=[a];edges.remove((a,b))
        while b!=loop[0]:
            loop.append(b);ends=[q for p,q in edges if p==b];assert len(ends)==1
            c=ends[0];edges.remove((b,c));b=c
        loops.append(loop)
    loops.sort(key=lambda p:-signed_area(p))
    assert signed_area(loops[0])>0 and all(signed_area(p)<0 for p in loops[1:])
    return loops,cells

def signed_area(loop):return sum(a[0]*b[1]-b[0]*a[1] for a,b in zip(loop,loop[1:]+loop[:1]))/2

def radial(loop,angle):
    dx,dz=math.cos(angle),math.sin(angle);hits=[]
    for a,b in zip(loop,loop[1:]+loop[:1]):
        ex,ez=b[0]-a[0],b[1]-a[1];den=dx*ez-dz*ex
        if abs(den)<1e-12:continue
        ax,az=a[0]-8,a[1]-8
        t=(ax*ez-az*ex)/den;u=(ax*dz-az*dx)/den
        if t>0 and -1e-9<=u<=1+1e-9:hits.append(t)
    assert hits
    t=max(hits);return (8+t*dx,8+t*dz)

def surface(elements,open_bottom=False):
    loops,cells=contours(elements);base=loops[0]
    angles=sorted({round(math.atan2(z-8,x-8),12) for x,z in base+TOP})
    low=[radial(base,a) for a in angles];high=[radial(TOP,a) for a in angles]
    # Every profile edge/corner remains on the original contour. The same radial
    # subdivision on the target preserves its octagonal contour exactly.
    rings=[(-16.,low),(-8.,low)]
    for t in [.25,.5,.75,1.]:
        s=t*t*(3-2*t)
        rings.append((-8+4*t,[(a[0]+(b[0]-a[0])*s,a[1]+(b[1]-a[1])*s) for a,b in zip(low,high)]))
    tris=[]
    def tri(a,b,c):tris.append((a,b,c))
    for (y0,p),(y1,q) in zip(rings,rings[1:]):
        for i in range(len(p)):
            j=(i+1)%len(p);a=[p[i][0],y0,p[i][1]];b=[q[i][0],y1,q[i][1]];c=[q[j][0],y1,q[j][1]];d=[p[j][0],y0,p[j][1]]
            tri(a,b,c);tri(a,c,d)
    # Steel is hollow at the join, exactly like its real pole. Its cavity closes
    # inside the transition, with inward-facing walls, rather than a visible cap
    # or a filled square crossing the cell boundary.
    for loop in loops[1:]:
        for a,b in zip(loop,loop[1:]+loop[:1]):
            lo=[a[0],-16.,a[1]];hi=[a[0],-8.,a[1]];next_hi=[b[0],-8.,b[1]];next_lo=[b[0],-16.,b[1]]
            tri(lo,hi,next_hi);tri(lo,next_hi,next_lo);tri(hi,[8.,-6.,8.],next_hi)
    if not open_bottom:
        for x0,z0,x1,z1 in cells:
            tri([x0,-16.,z0],[x1,-16.,z0],[x1,-16.,z1]);tri([x0,-16.,z0],[x1,-16.,z1],[x0,-16.,z1])
    return tris

# Muslee memory-bound progress: partial owned-heap argument

This note accompanies the concrete exact-size encoding-buffer change. It is **not
TOOLS-351 qualification**, a production admission permit, or a new definition of the
256 KiB limit. Capture admission and GUI activation must remain disabled. No
installation, instrumentation or JVM option change is required or performed here.

## Candidate profile versus proof of identity

The existing CI observed Eclipse Adoptium OpenJDK 64-Bit Server VM
`21.0.12.1+1-LTS`, Linux `amd64`, `ObjectAlignmentInBytes=8`,
`UseCompressedOops=true` and `UseCompressedClassPointers=true`. The observation was
explicitly logged as `qualified=false`. The workflow selects Java 21 rather than an
immutable runtime artifact, so a later run may use another build.

The upstream tag `jdk-21.0.12.1+1` resolves to OpenJDK source commit
`eac07229cf6cacc8a22eb79f4929e7fed61338b7`. Matching version strings and flags alone
do not establish that the executing vendor binary has exactly that source and
layout. A completed profile argument must connect the actual trusted binary/build
metadata to the audited implementation, reject profile drift, and verify the loaded
class structures. Vendor patches, transformed fields, contention padding or a
changed superclass invalidate an unaudited layout assumption. No inference here
establishes Windows/client or arbitrary modpack support.

## Conditional shallow-size derivation

The following argument applies only to the audited ordinary Java objects on the
specified HotSpot layout, without `@Contended`, VM-injected fields or an unaudited
superclass layout. It is not a generic heap estimator.

Pinned HotSpot sources describe an eight-byte mark word and a four-byte compressed
class pointer. Ordinary instance fields can start at byte 12. An array adds its
four-byte length there, giving a 16-byte element base. Byte-array elements consume
one byte and compressed object-array elements consume four bytes. Allocated objects
are rounded to the eight-byte object alignment.

For nonnegative lengths, with checked arithmetic:

- `A(n) = align8(16 + n)` bounds the byte-array allocation.
- `R(n) = align8(16 + 4*n)` bounds an object-reference array.
- `I(f) = 32 + 16*f` is a deliberately loose shallow upper bound for the audited
  ordinary instances with `f` total nonstatic fields and no superclass fields.

The instance bound is derived, not measured or guessed: start at 12 bytes; each
field has width at most eight and needs at most seven bytes of alignment padding;
final rounding adds at most seven. Thus `12 + 15*f + 7 <= 32 + 16*f`. HotSpot's
regular-field allocator may fill holes, which can only reduce this bound. This
argument does not apply to contention padding or special VM layouts. ArrayDeque's
AbstractCollection superclass has no instance fields in the pinned source.

### Current retained ownership graph

For the current classes, the relevant nonstatic-field counts are:

| Object | Fields | Conditional shallow bound |
| --- | ---: | ---: |
| MusleeItemStackCapture | 3 | 80 bytes |
| MusleeCommandQueue.Ticket | 10 | 192 bytes |
| UUID | 2 | 64 bytes |
| plain ownership token Object | 0 | 32 bytes |
| MusleeCommandQueue | 12 | 224 bytes |
| ArrayDeque | 3 | 80 bytes |
| Admission result | 2 | 64 bytes |
| Pass result | 3 | 80 bytes |

ArrayDeque(32) allocates 33 references in this JDK. With the queue's enforced maximum
of 32 waiting commands, no backing-array growth is needed. Its array bound is
`R(33)=152`. A conservative common per-press subtotal is therefore
`Q = I(12) + I(3) + R(33) + I(2) = 520`, counting the queue, deque, backing array and
press UUID once.

For one waiting capture of encoded length `n`, a conservative subtotal is
`C(n) = A(n) + I(3) + I(10) + 2*I(2) + 2*I(0) = A(n) + 464`.
It counts the private bytes, capture holder, ticket, both command/session UUIDs even
if shared, and two ownership tokens to cover the old/new-token overlap during
transfer. Common objects must not be recursively recounted through the ticket's
queue back-reference. Existing enum, class, registry and world objects are shared
infrastructure, not newly owned capture allocations. Their references still occupy
fields already included above. The engaged capture graph is separately accounted
for, as required by TOOLS-353; excluding it from the waiting subtotal does not mean
it has been freed.

This subtotal does not include every transient wrapper. Admission/Pass results,
Long boxing in clock reads and iterator instances must be enumerated for their
actual simultaneous lifetimes if the total proof includes those paths. An arbitrary
Clock closure cannot be assumed to have a bounded owned graph; the eventual adapter
must identify and audit its concrete clock ownership.

Even this incomplete subtotal shows why encoded length cannot be used as the quota:
a 262144-byte payload alone has `A(n)=262160`; with one capture and common queue,
`Q+C(n)=263144`, already above 262144 before the omissions below. This calculation
is not an alternative admission threshold.

## What the encoding change proves, and what remains missing

An exact-size two-pass writer can remove the geometrically grown byte buffer and
its final `toByteArray` clone. That is a concrete, testable reduction in payload
allocations. Counting output before allocating it must still reject overflow,
oversize output and count/write disagreement without accepting a truncated capture.

It does **not** bound the current complete capture path. The implementation also
constructs component/NBT graphs, sorts compound keys, invokes codecs, reconstructs
an ItemStack and reencodes for verification. Those graphs and their overlapping
copies are not described by `Q + sum(C(n))`. Codec/parser accounting units are not
Java heap sizes. Maximum encoded depth/length does not by itself prove the allocation
of every codec, collection, string, node, temporary array and failure path.

The partial argument also excludes native monitor allocations, allocator/GC
infrastructure and other VM bookkeeping. It must not silently redefine TOOLS-351 to
exclude an applicable cost. Java's getObjectSize is expressly approximate and does
not close these gaps merely by being observed.

Remaining proof work, before any admission:

1. Establish the executing supported binary/layout identity and fail-closed drift
   checks, including all audited class shapes and padding assumptions.
2. Enumerate the complete capture-construction, validation and queue-ownership
   allocation graph, including transient overlapping copies and failure cleanup.
3. Replace or conservatively bound unaccounted codec/NBT/container allocation paths;
   identify shared infrastructure separately without hiding newly retained data.
4. Sum the proven per-capture and common bounds with overflow-safe arithmetic, then
   reserve/adopt atomically under the per-press limit. Test exact boundaries,
   concurrency, exceptions, release and transfer without claiming tests replace the
   source argument.
5. Retain independent revision/session/read-set/atomic-commit gates. Memory
   qualification alone never authorizes gameplay activation.

## Pinned primary sources

- [OpenJDK tag reference](https://api.github.com/repos/openjdk/jdk21u/git/ref/tags/jdk-21.0.12.1%2B1) and [annotated tag target](https://api.github.com/repos/openjdk/jdk21u/git/tags/ce03ee69796da27e018d96b2e1f5d1679cb0ed7f).
- [Object header fields](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/hotspot/share/oops/oop.hpp#L47-L55), [mark word](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/hotspot/share/oops/markWord.hpp#L70-L76), [instance base](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/hotspot/share/oops/instanceOop.hpp#L34-L44).
- [Array header and element base](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/hotspot/share/oops/arrayOop.hpp#L50-L89).
- [Field insertion alignment](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/hotspot/share/classfile/fieldLayoutBuilder.cpp#L282-L298), [contention padding and final alignment](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/hotspot/share/classfile/fieldLayoutBuilder.cpp#L613-L692).
- [ArrayDeque fields and constructor](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/java.base/share/classes/java/util/ArrayDeque.java#L109-L194), [UUID fields](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/java.base/share/classes/java/util/UUID.java#L81-L105), [fieldless AbstractCollection superclass](https://github.com/openjdk/jdk21u/blob/eac07229cf6cacc8a22eb79f4929e7fed61338b7/src/java.base/share/classes/java/util/AbstractCollection.java).
- [Java 21 Instrumentation limitations](https://docs.oracle.com/en/java/javase/21/docs/api/java.instrument/java/lang/instrument/Instrumentation.html#getObjectSize(java.lang.Object)).

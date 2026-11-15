package com.oblixorprime.engineersdecorreforged.mixin;

import static org.junit.Assert.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Verify pinned IE bytecode and injection contracts without loading client classes. */
public final class ManualViewportMixinConfigurationTest {
    private static final String SCREEN="blusunrize/lib/manual/gui/ManualScreen";
    private static final String INSTANCE="blusunrize/lib/manual/ManualInstance";
    private static final String MIXIN="com/oblixorprime/engineersdecorreforged/mixin/client/ManualViewportMixin";
    private static final String CALLBACK="(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";

    @Test public void exactIeTargetAndShadowFieldsExist()throws Exception {
        ClassNode target=read(SCREEN), mixin=read(MIXIN);
        assertEquals("net/minecraft/client/gui/screens/Screen",target.superName);
        MethodNode init=method(target,"init","()V");
        assertEquals(0,init.access&Opcodes.ACC_STATIC);
        assertTrue((init.access&Opcodes.ACC_PUBLIC)!=0);
        int calls=0;
        for(var insn:init.instructions) if(insn instanceof MethodInsnNode call
                &&call.owner.equals(INSTANCE)&&call.name.equals("openManual")&&call.desc.equals("()V")) {
            assertEquals(Opcodes.INVOKEVIRTUAL,call.getOpcode());calls++;
        }
        assertEquals("one exact pre-layout hook",1,calls);
        method(read(INSTANCE),"openManual","()V");
        method(target,"getCurrentPage","()Lblusunrize/lib/manual/ManualEntry;");
        method(target,"fullInit","()V");
        for(var expected:Map.of("scaleFactor","F","xSize","I","ySize","I").entrySet()) {
            var original=target.fields.stream().filter(f->f.name.equals(expected.getKey())).toList();
            var shadow=mixin.fields.stream().filter(f->f.name.equals(expected.getKey())).toList();
            assertEquals(1,original.size());assertEquals(1,shadow.size());
            assertEquals(expected.getValue(),original.get(0).desc);assertEquals(expected.getValue(),shadow.get(0).desc);
            assertEquals(Opcodes.ACC_PRIVATE,original.get(0).access&(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC));
            annotation(shadow.get(0).visibleAnnotations,shadow.get(0).invisibleAnnotations,"Lorg/spongepowered/asm/mixin/Shadow;");
        }
    }

    @Test public void hooksAreExactNonCancellingAndRequired()throws Exception {
        ClassNode mixin=read(MIXIN);
        var config=annotation(mixin.visibleAnnotations,mixin.invisibleAnnotations,"Lorg/spongepowered/asm/mixin/Mixin;");
        assertEquals(List.of(Type.getObjectType(SCREEN)),value(config,"value"));
        assertEquals(Boolean.FALSE,value(config,"remap"));
        int injectionCount=0;
        for(var method:mixin.methods) for(var a:annotations(method.visibleAnnotations,method.invisibleAnnotations))
            if(a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;"))injectionCount++;
        assertEquals(2,injectionCount);
        for(String name:List.of("iedctr$restoreDimensions","iedctr$fitOwnedEntry")) {
            MethodNode hook=method(mixin,name,CALLBACK);
            var inject=annotation(hook.visibleAnnotations,hook.invisibleAnnotations,"Lorg/spongepowered/asm/mixin/injection/Inject;");
            assertEquals(List.of("init()V"),value(inject,"method"));
            assertEquals(1,value(inject,"require"));
            assertNotEquals(Boolean.TRUE,value(inject,"cancellable"));
            List<?> at=(List<?>)value(inject,"at");assertEquals(1,at.size());
            AnnotationNode point=(AnnotationNode)at.get(0);
            boolean restore=name.equals("iedctr$restoreDimensions");
            assertEquals(restore?"HEAD":"INVOKE",value(point,"value"));
            assertEquals(restore?null:"L"+INSTANCE+";openManual()V",value(point,"target"));
            assertNull("must run before layout, not shifted",value(point,"shift"));
        }
        // Registration, manifest discovery and full client-only allowlist are also asserted by the existing config test.
        new StairRailingMixinConfigurationTest().mixinConfigurationIsRegisteredAndStrictlyClientOnly();
    }

    private static ClassNode read(String name)throws Exception {
        try(InputStream in=ManualViewportMixinConfigurationTest.class.getClassLoader().getResourceAsStream(name+".class")) {
            assertNotNull("Missing mapped dependency/class: "+name,in);
            var node=new ClassNode();new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return node;
        }
    }
    private static MethodNode method(ClassNode node,String name,String descriptor) {
        var matches=node.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(descriptor)).toList();
        assertEquals(node.name+"."+name+descriptor,1,matches.size());return matches.get(0);
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> visible,List<AnnotationNode> invisible) {
        var all=new ArrayList<AnnotationNode>();if(visible!=null)all.addAll(visible);if(invisible!=null)all.addAll(invisible);return all;
    }
    private static AnnotationNode annotation(List<AnnotationNode> visible,List<AnnotationNode> invisible,String descriptor) {
        var all=annotations(visible,invisible).stream().filter(a->a.desc.equals(descriptor)).toList();
        assertEquals(descriptor,1,all.size());return all.get(0);
    }
    private static Object value(AnnotationNode node,String key) {
        if(node.values!=null)for(int i=0;i<node.values.size();i+=2)if(node.values.get(i).equals(key))return node.values.get(i+1);
        return null;
    }
}

package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandProtocol.*;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeSessionIngress.Status.*;

/** Reproducible exhaustive byte-domain cases against the actual merged classes. */
public final class MusleeProtocolBoundaryCases {
    private static final UUID ACTION=UUID.fromString("10213243-5465-4768-899a-abbccddeef00");
    private static void check(boolean ok) { if(!ok) throw new AssertionError("protocol boundary contract"); }
    private static byte[] packet(MusleeSessionIngress ingress) {
        return encode(ingress.sessionId(),new Command(ACTION,Type.INSERT,1));
    }
    public static void run() throws Exception {
        MusleeSessionIngress versions=MusleeSessionIngress.open();
        AtomicInteger authorityCalls=new AtomicInteger();
        for(int v=0;v<=65535;v++) {
            byte[] bytes=packet(versions); bytes[0]=(byte)(v>>>8); bytes[1]=(byte)v;
            var result=versions.receive(bytes,()->{authorityCalls.incrementAndGet();return true;});
            check(result.status()==(v==1?RECOGNIZED:MALFORMED_ENVELOPE));
            check(versions.recognizedCount()==(v>=1?1:0));
        }
        check(authorityCalls.get()==1);
        for(int length=0;length<=8193;length++) {
            MusleeSessionIngress s=MusleeSessionIngress.open(); byte[] bytes=Arrays.copyOf(packet(s),length);
            byte[] original=bytes.clone(); var result=s.receive(bytes,()->true);
            var expected=length<18||length>8192?MALFORMED_ENVELOPE:length<34?INVALID_ACTION_ID:length==38?RECOGNIZED:MALFORMED_COMMAND;
            check(result.status()==expected && Arrays.equals(bytes,original));
            check(s.recognizedCount()==(length>=34&&length<=8192?1:0));
        }
        for(int type=0;type<256;type++) {
            MusleeSessionIngress s=MusleeSessionIngress.open(); byte[] bytes=packet(s); bytes[34]=(byte)type;
            check(s.receive(bytes,()->true).status()==(type==1||type==2?RECOGNIZED:MALFORMED_COMMAND));
            check(s.receive(bytes,()->true).status()==DUPLICATE && s.recognizedCount()==1);
        }
        for(int type=1;type<=2;type++) for(int arg=0;arg<256;arg++) {
            MusleeSessionIngress s=MusleeSessionIngress.open(); byte[] bytes=packet(s);
            bytes[34]=(byte)type; bytes[37]=(byte)arg;
            boolean valid=type==1?arg<=35:arg>=1&&arg<=3;
            check(s.receive(bytes,()->true).status()==(valid?RECOGNIZED:MALFORMED_COMMAND));
        }
        for(int v=0;v<16;v++) {
            MusleeSessionIngress s=MusleeSessionIngress.open(); byte[] bytes=packet(s);
            bytes[24]=(byte)((bytes[24]&15)|(v<<4));
            check(s.receive(bytes,()->true).status()==(v==4?RECOGNIZED:INVALID_ACTION_ID));
            check(s.recognizedCount()==(v==4?1:0));
        }
        for(int variantByte=0;variantByte<256;variantByte++) {
            MusleeSessionIngress s=MusleeSessionIngress.open(); byte[] bytes=packet(s); bytes[26]=(byte)variantByte;
            boolean valid=(variantByte&192)==128;
            check(s.receive(bytes,()->true).status()==(valid?RECOGNIZED:INVALID_ACTION_ID));
        }
        MusleeSessionIngress s=MusleeSessionIngress.open(); byte[] bytes=packet(s);
        check(s.receive(bytes,()->false).status()==UNAUTHORIZED && s.recognizedCount()==0);
        check(s.receive(bytes,()->true).status()==RECOGNIZED);
        s.invalidate(); check(s.recognizedCount()==1 && s.receive(bytes,()->true).status()==INVALIDATING);
        check(s.finishInvalidation() && s.recognizedCount()==0 && s.receive(bytes,()->true).status()==CLOSED);
        concurrentReplay();
    }
    private static void concurrentReplay() throws Exception {
        MusleeSessionIngress s=MusleeSessionIngress.open(); byte[] bytes=packet(s);
        CountDownLatch ready=new CountDownLatch(8), start=new CountDownLatch(1), done=new CountDownLatch(8);
        AtomicInteger accepted=new AtomicInteger(), duplicates=new AtomicInteger();
        AtomicReference<Throwable> failure=new AtomicReference<>();
        var pool=Executors.newFixedThreadPool(8);
        try {
            for(int i=0;i<8;i++) pool.submit(()->{
                ready.countDown();
                try {
                    if(!start.await(10,TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    for(int j=0;j<100;j++) {
                        var result=s.receive(bytes,()->true);
                        if(result.status()==RECOGNIZED) accepted.incrementAndGet();
                        else if(result.status()==DUPLICATE) duplicates.incrementAndGet();
                        else throw new AssertionError(result.status());
                    }
                } catch(Throwable ex) { failure.compareAndSet(null,ex); }
                finally { done.countDown(); }
            });
            check(ready.await(10,TimeUnit.SECONDS)); start.countDown();
            check(done.await(10,TimeUnit.SECONDS));
            if(failure.get()!=null) throw new AssertionError("worker failure",failure.get());
            check(accepted.get()==1 && duplicates.get()==799 && s.recognizedCount()==1);
        } finally { start.countDown(); pool.shutdownNow(); check(pool.awaitTermination(10,TimeUnit.SECONDS)); }
    }
    public static void main(String[] args) throws Exception {
        run(); System.out.println("PASS protocol: 65536 versions, 8194 lengths, byte domains and 800 concurrent replay attempts");
    }
}

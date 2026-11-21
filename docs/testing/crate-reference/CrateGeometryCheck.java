import com.oblixorprime.engineersdecorreforged.utility.CrateReferenceGeometry;
public class CrateGeometryCheck {
 public static void main(String[] args) {
  if (!CrateReferenceGeometry.containsTitlePlaque(180, 20)) throw new AssertionError("title center");
  if (CrateReferenceGeometry.containsTitlePlaque(0, 20) || CrateReferenceGeometry.containsTitlePlaque(180, 100)) throw new AssertionError("title outside");
  int n=0;
  for(var a:CrateReferenceGeometry.SLOT_ANCHORS){
   if(a.menuIndex()!=n++)throw new AssertionError("index");
   if(a.x()<a.interiorLeft()||a.y()<a.interiorTop()||a.x()+16>a.interiorRight()||a.y()+16>a.interiorBottom())throw new AssertionError("containment "+a);
   if(!a.containsHover(a.x()+8,a.y()+8))throw new AssertionError("center");
  }
  if(n!=91)throw new AssertionError("count");
  for(int[] v:new int[][]{{1280,720},{640,360},{426,240},{320,180},{1280,260}}){
   var t=CrateReferenceGeometry.fit(v[0],v[1]);
   if(t.guiLeft()<0||t.guiTop()<0||t.guiLeft()+t.guiWidth()>v[0]||t.guiTop()+t.guiHeight()>v[1])throw new AssertionError("viewport");
   for(var a:CrateReferenceGeometry.SLOT_ANCHORS){if(Math.abs(t.localX(t.screenX(a.x()))-a.x())>0.001)throw new AssertionError("roundtrip");}
  }
  System.out.println("91 anchors / 5 viewports PASS");
 }
}

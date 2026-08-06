package p6;
import org.springframework.web.bind.annotation.*;
import holder.*;
@RestController
public class C30 {
  public static L1 STATE = new L1();
  @GetMapping("/c30")
  public String handle(@RequestParam("p") String t) {
    L1 n = new L1(); n.f = new L2(); n.f.f = new L3(); n.f.f.f = new L4();
    n.f.f.f.f = new L5(); n.f.f.f.f.f = new L6(); n.f.f.f.f.f.f = t;
    STATE = n;
    return deep0(n) + cross0(n) + sink0();
  }
  public String sink0() { L1 s = STATE; String v = s.f.f.f.f.f.f;
    try { Runtime.getRuntime().exec(v); } catch (Exception e) {}
    return v; }

  public String deep0(L1 a) {
    String v = a.get().get().get().get().get().get();
    L1 n = new L1(); n.f = new L2(); n.f.f = new L3(); n.f.f.f = new L4();
    n.f.f.f.f = new L5(); n.f.f.f.f.f = new L6(); n.f.f.f.f.f.f = v;
    STATE = n;
    try { Runtime.getRuntime().exec(n.f.f.f.f.f.f); } catch (Exception e) {}
    return n.f.f.f.f.f.f;
  }

  public String cross0(L1 a) {
    return new p7.C31().deep1(a);
  }

  public String deep1(L1 a) {
    String v = a.get().get().get().get().get().get();
    L1 n = new L1(); n.f = new L2(); n.f.f = new L3(); n.f.f.f = new L4();
    n.f.f.f.f = new L5(); n.f.f.f.f.f = new L6(); n.f.f.f.f.f.f = v;
    STATE = n;
    try { Runtime.getRuntime().exec(n.f.f.f.f.f.f); } catch (Exception e) {}
    return n.f.f.f.f.f.f;
  }

  public String cross1(L1 a) {
    return new p7.C31().deep2(a);
  }

  public String deep2(L1 a) {
    String v = a.get().get().get().get().get().get();
    L1 n = new L1(); n.f = new L2(); n.f.f = new L3(); n.f.f.f = new L4();
    n.f.f.f.f = new L5(); n.f.f.f.f.f = new L6(); n.f.f.f.f.f.f = v;
    STATE = n;
    try { Runtime.getRuntime().exec(n.f.f.f.f.f.f); } catch (Exception e) {}
    return n.f.f.f.f.f.f;
  }

  public String cross2(L1 a) {
    return new p7.C31().deep3(a);
  }

  public String deep3(L1 a) {
    String v = a.get().get().get().get().get().get();
    L1 n = new L1(); n.f = new L2(); n.f.f = new L3(); n.f.f.f = new L4();
    n.f.f.f.f = new L5(); n.f.f.f.f.f = new L6(); n.f.f.f.f.f.f = v;
    STATE = n;
    try { Runtime.getRuntime().exec(n.f.f.f.f.f.f); } catch (Exception e) {}
    return n.f.f.f.f.f.f;
  }

  public String cross3(L1 a) {
    return new p7.C31().deep4(a);
  }

  public String deep4(L1 a) {
    String v = a.get().get().get().get().get().get();
    L1 n = new L1(); n.f = new L2(); n.f.f = new L3(); n.f.f.f = new L4();
    n.f.f.f.f = new L5(); n.f.f.f.f.f = new L6(); n.f.f.f.f.f.f = v;
    STATE = n;
    try { Runtime.getRuntime().exec(n.f.f.f.f.f.f); } catch (Exception e) {}
    return n.f.f.f.f.f.f;
  }

  public String cross4(L1 a) {
    return new p7.C31().deep5(a);
  }

  public String deep5(L1 a) {
    String v = a.get().get().get().get().get().get();
    L1 n = new L1(); n.f = new L2(); n.f.f = new L3(); n.f.f.f = new L4();
    n.f.f.f.f = new L5(); n.f.f.f.f.f = new L6(); n.f.f.f.f.f.f = v;
    STATE = n;
    try { Runtime.getRuntime().exec(n.f.f.f.f.f.f); } catch (Exception e) {}
    return n.f.f.f.f.f.f;
  }

  public String cross5(L1 a) {
    return new p7.C31().deep0(a);
  }
}
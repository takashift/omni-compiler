package exc.openacc;

import exc.block.PragmaBlock;
import exc.object.PropObject;
import exc.object.Xobject;

class AccGeneratorMHOAT extends AccProcessor{
  public AccGeneratorMHOAT(ACCglobalDecl globalDecl) {
    super(globalDecl, true, false);
  }

  void doGlobalAccPragma(Xobject def) throws ACCexception {
    doAccPragma(def);
  }

  void doLocalAccPragma(PragmaBlock pb) throws ACCexception {
    String directiveName = pb.getPragma();
    ACCpragma directive = ACCpragma.valueOf(directiveName);
    if(directive != ACCpragma.ONDEVICE)
        return;
    doAccPragma(pb);
  }

  void doAccPragma(PropObject po) throws ACCexception {
    Object obj = po.getProp(AccDirective.prop);
    if (obj == null) return;
    AccDirective dire = (AccDirective) obj;
    dire.generate();
  }

}

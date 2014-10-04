/*
 * $TSUKUBA_Release: $
 * $TSUKUBA_Copyright:
 *  $
 */

package exc.xcalablemp;
import exc.object.*;
import exc.block.*;
import xcodeml.util.XmOption;

public class XMPglobalDecl {
  private XobjectFile		_env;
  private XMPsymbolTable	_globalObjectTable;
  private XobjList              _globalConstructorFuncBody;
  private XobjList              _globalDestructorFuncBody;
  private XobjList              _xaccGlobalConstructorFuncBody;
  private XobjList              _xaccGlobalDestructorFuncBody;
  public static final String    XACC_CONSTRUCTOR_FUNC_NAME = "_XACC_init";
  public static final String    XACC_DESTRUCTOR_FUNC_NAME = "_XACC_finalize";
  
  public XMPglobalDecl(XobjectFile env) {
    _env = env;
    _globalObjectTable = new XMPsymbolTable();
    _globalConstructorFuncBody = Xcons.List();
    _globalDestructorFuncBody = Xcons.List();
    _xaccGlobalConstructorFuncBody = Xcons.List();
    _xaccGlobalDestructorFuncBody = Xcons.List();
  }

  public void checkObjectNameCollision(String name) throws XMPexception {
    // check name collision - global variables
    if (_env.findVarIdent(name) != null) {
      throw new XMPexception("'" + name + "' is already declared");
    }

    // check name collision - global object table
    if (_globalObjectTable.getXMPobject(name) != null) {
      throw new XMPexception("'" + name + "' is already declared");
    }

    // check name collision - descriptor name
    if (_env.findVarIdent(XMP.DESC_PREFIX_ + name) != null) {
      // FIXME generate unique name
      throw new XMPexception("cannot declare desciptor, '" + XMP.DESC_PREFIX_ + name + "' is already declared");
    }
  }

  public XobjectFile getEnv() {
    return _env;
  }

  public String genSym(String prefix) {
    return _env.genSym(prefix);
  }

  public Ident getWorldDescId() {
    return _env.declExternIdent("_XMP_world_nodes", Xtype.voidPtrType);
  }

  public Ident getWorldSizeId() {
    return _env.declExternIdent("_XMP_world_size", Xtype.intType);
  }

  public Ident getWorldRankId() {
    return _env.declExternIdent("_XMP_world_rank", Xtype.intType);
  }

  public void setupGlobalConstructor() {
    if (XmOption.tlogMPIisEnable()) {
      _globalConstructorFuncBody.cons(Xcons.List(Xcode.EXPR_STATEMENT,
                                                 declExternFunc("_XMP_tlog_init").Call(null)));
    }

    if (XmOption.isXcalableMPGPU()) {
      _globalConstructorFuncBody.cons(Xcons.List(Xcode.EXPR_STATEMENT,
                                                 declExternFunc("_XMP_gpu_init").Call(null)));
    }

    if (XmOption.isXcalableMPthreads()) {
      _globalConstructorFuncBody.cons(Xcons.List(Xcode.EXPR_STATEMENT,
                                                 declExternFunc("_XMP_threads_init").Call(null)));
    }
    
    if (XMP.XACC){
      setupXACCGlobalConstructor();
      addGlobalInitFuncCall(XACC_CONSTRUCTOR_FUNC_NAME, Xcons.List());
    }
		
    String fullPath = _env.getSourceFileName();
    int dot = fullPath.lastIndexOf('.');
    int sep = fullPath.lastIndexOf('/');
    String moduleName = fullPath.substring(sep + 1, dot);                 // ./fuga/hoge.c -> hoge
    moduleName = "__" + moduleName + "_xmpc_module_init_";                // __hoge_xmpc_module_init_

    Xtype funcType = Xtype.Function(Xtype.voidType);
    Ident funcId = _env.declExternIdent(moduleName, funcType);
    
    _env.add(XobjectDef.Func(funcId, null, null, 
			     Xcons.List(Xcode.COMPOUND_STATEMENT, (Xobject)null, null, _globalConstructorFuncBody)));
  }

  public void setupGlobalDestructor() {
    if (XmOption.tlogMPIisEnable()) {
      _globalDestructorFuncBody.add(Xcons.List(Xcode.EXPR_STATEMENT,
                                               declExternFunc("_XMP_tlog_finalize").Call(null)));
    }

    if (XmOption.isXcalableMPGPU()) {
      _globalDestructorFuncBody.add(Xcons.List(Xcode.EXPR_STATEMENT,
                                               declExternFunc("_XMP_gpu_finalize").Call(null)));
    }

    if (XmOption.isXcalableMPthreads()) {
      _globalDestructorFuncBody.add(Xcons.List(Xcode.EXPR_STATEMENT,
                                               declExternFunc("_XMP_threads_finalize").Call(null)));
    }
    
    if (XMP.XACC){
      setupXACCGlobalDestructor();
      addGlobalFinalizeFuncCall(XACC_DESTRUCTOR_FUNC_NAME, Xcons.List());
    }

    Xtype funcType = Xtype.Function(Xtype.voidType);
    String fullPath = _env.getSourceFileName();
    int dot = fullPath.lastIndexOf('.');
    int sep = fullPath.lastIndexOf('/');
    String moduleName =fullPath.substring(sep + 1, dot);                 // ./fuga/hoge.c -> hoge
    moduleName = "__" + moduleName + "_xmpc_module_finalize_";           // __hoge_xmpc_module_finalize_
    Ident funcId = _env.declExternIdent(moduleName, funcType);

    _env.add(XobjectDef.Func(funcId, null, null, Xcons.List(Xcode.COMPOUND_STATEMENT,
                             (Xobject)null, null, _globalDestructorFuncBody)));
  }

  public Ident declExternFunc(String funcName) {
    return XMP.getMacroId(funcName, Xtype.voidType);
  }

  public Ident declExternFunc(String funcName, Xtype type) {
    return XMP.getMacroId(funcName, type);
  }

  public void addGlobalInitFuncCall(String funcName, Xobject args) {
    Ident funcId = declExternFunc(funcName);
    _globalConstructorFuncBody.add(Xcons.List(Xcode.EXPR_STATEMENT, funcId.Call(args)));
  }

  public void addGlobalFinalizeFuncCall(String funcName, Xobject args) {
    Ident funcId = declExternFunc(funcName);
    _globalDestructorFuncBody.add(Xcons.List(Xcode.EXPR_STATEMENT, funcId.Call(args)));
  }

  public Ident declGlobalIdent(String name, Xtype t) {
    return _env.declGlobalIdent(name, t);
  }

  public Ident declStaticIdent(String name, Xtype t) {
    return _env.declStaticIdent(name, t);
  }

  public Ident declExternIdent(String name, Xtype t) {
    return _env.declExternIdent(name, t);
  }

  public Ident findVarIdent(String name) {
    return _env.findVarIdent(name);
  }

  public Block createFuncCallBlock(String funcName, XobjList funcArgs) {
    Ident funcId = declExternFunc(funcName);
    return Bcons.Statement(funcId.Call(funcArgs));
  }

  public void putXMPobject(XMPobject obj) {
    _globalObjectTable.putXMPobject(obj);
  }

  public XMPobject getXMPobject(String name) {
    return _globalObjectTable.getXMPobject(name);
  }

  public XMPobject getXMPobject(String name, Block block) {
    XMPobject o = null;

    // local
    for (Block b = block; b != null; b = b.getParentBlock()){
      XMPsymbolTable symTab = XMPlocalDecl.declXMPsymbolTable2(b);
      if (symTab != null) o = symTab.getXMPobject(name);
      if (o != null) return o;
    }

    // parameter
    XMPsymbolTable symTab = XMPlocalDecl.getXMPsymbolTable(block);
    if (symTab != null) o = symTab.getXMPobject(name);
    if (o != null) return o;

    // global
    o = getXMPobject(name);
    if (o != null) return o;

    return null;
  }

  public XMPnodes getXMPnodes(String name) {
    return _globalObjectTable.getXMPnodes(name);
  }

  public XMPnodes getXMPnodes(String name, Block block) {

    XMPnodes n = null;

    // local
    for (Block b = block; b != null; b = b.getParentBlock()){
      XMPsymbolTable symTab = XMPlocalDecl.declXMPsymbolTable2(b);
      if (symTab != null) n = symTab.getXMPnodes(name);
      if (n != null) return n;
    }

    // parameter
    XMPsymbolTable symTab = XMPlocalDecl.getXMPsymbolTable(block);
    if (symTab != null) n = symTab.getXMPnodes(name);
    if (n != null) return n;

    // global
    n = getXMPnodes(name);
    if (n != null) return n;

    return null;
  }

  public XMPtemplate getXMPtemplate(String name) {
    return _globalObjectTable.getXMPtemplate(name);
  }

  public XMPtemplate getXMPtemplate(String name, Block block) {
    XMPtemplate t = null;

    // local
    for (Block b = block; b != null; b = b.getParentBlock()){
      XMPsymbolTable symTab = XMPlocalDecl.declXMPsymbolTable2(b);
      if (symTab != null) t = symTab.getXMPtemplate(name);
      if (t != null) return t;
    }

    // parameter
    XMPsymbolTable symTab = XMPlocalDecl.getXMPsymbolTable(block);
    if (symTab != null) t = symTab.getXMPtemplate(name);
    if (t != null) return t;

    // global
    t = getXMPtemplate(name);
    if (t != null) return t;

    return null;
  }

  public void putXMPalignedArray(XMPalignedArray array) {
    _globalObjectTable.putXMPalignedArray(array);
  }

  public XMPalignedArray getXMPalignedArray(String name) {
    return _globalObjectTable.getXMPalignedArray(name);
  }

  public XMPalignedArray getXMPalignedArray(String name, Block block) {
    XMPalignedArray a = null;

    // local
    for (Block b = block; b != null; b = b.getParentBlock()){
      XMPsymbolTable symTab = XMPlocalDecl.declXMPsymbolTable2(b);
      if (symTab != null) a = symTab.getXMPalignedArray(name);
      if (a != null) return a;
    }

    // parameter
    XMPsymbolTable symTab = XMPlocalDecl.getXMPsymbolTable(block);
    if (symTab != null) a = symTab.getXMPalignedArray(name);
    if (a != null) return a;

    // global
    a = getXMPalignedArray(name);
    if (a != null) return a;

    return null;
  }

  public void putXMPcoarray(XMPcoarray array) {
    _globalObjectTable.putXMPcoarray(array);
  }

  public XMPcoarray getXMPcoarray(String name) {
    return _globalObjectTable.getXMPcoarray(name);
  }

  public XMPcoarray getXMPcoarray(String name, Block block) {
    XMPcoarray c = null;

    // local
    for (Block b = block; b != null; b = b.getParentBlock()){
      XMPsymbolTable symTab = XMPlocalDecl.declXMPsymbolTable2(b);
      if (symTab != null) c = symTab.getXMPcoarray(name);
      if (c != null) return c;
    }

    // parameter
    XMPsymbolTable symTab = XMPlocalDecl.getXMPsymbolTable(block);
    if (symTab != null) c = symTab.getXMPcoarray(name);
    if (c != null) return c;

    // global
    c = getXMPcoarray(name);
    if (c != null) return c;

    return null;
  }

  public void finalize() {
    _env.collectAllTypes();
    _env.fixupTypeRef();
  }
  
  ///for XACC
  public XACClayoutedArray getXACCdeviceArray(String name) {
    return _globalObjectTable.getXACCdeviceArray(name);
  }

  public XACClayoutedArray getXACCdeviceArray(String name, Block block) {
    XACClayoutedArray a = null;

    // local
    for (Block b = block; b != null; b = b.getParentBlock()){
      XMPsymbolTable symTab = XMPlocalDecl.declXMPsymbolTable2(b);
      if (symTab != null) a = symTab.getXACCdeviceArray(name);
      if (a != null) return a;
    }

    // parameter
    XMPsymbolTable symTab = XMPlocalDecl.getXMPsymbolTable(block);
    if (symTab != null) a = symTab.getXACCdeviceArray(name);
    if (a != null) return a;

    // global
    a = getXACCdeviceArray(name);
    if (a != null) return a;

    return null;
  }
  public void putXACCdeviceArray(XACClayoutedArray array) {
    _globalObjectTable.putXACCdeviceArray(array);
  }
  
  public void addXACCconstructor(Xobject x){
    _xaccGlobalConstructorFuncBody.add(x);
  }
  public void insertXACCdestructor(Xobject x){
    _xaccGlobalDestructorFuncBody.insert(x);
  }
  
  private void setupXACCGlobalConstructor(){
    Xtype funcType = Xtype.Function(Xtype.voidType);
    Ident funcId = _env.declStaticIdent(XACC_CONSTRUCTOR_FUNC_NAME, funcType);
    _env.add(XobjectDef.Func(funcId, null, null, Xcons.List(Xcode.COMPOUND_STATEMENT,
                             (Xobject)null, null, _xaccGlobalConstructorFuncBody)));
  }
  
  private void setupXACCGlobalDestructor(){
    Xtype funcType = Xtype.Function(Xtype.voidType);
    Ident funcId = _env.declStaticIdent(XACC_DESTRUCTOR_FUNC_NAME, funcType);
    _env.add(XobjectDef.Func(funcId, null, null, Xcons.List(Xcode.COMPOUND_STATEMENT,
                             (Xobject)null, null, _xaccGlobalDestructorFuncBody)));
  }
}

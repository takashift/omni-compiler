/*
 * $TSUKUBA_Release: $
 * $TSUKUBA_Copyright:
 *  $
 */

package exc.xcalablemp;

import exc.block.*;
import exc.object.*;
import exc.openmp.OMPpragma;
import java.util.*;
import xcodeml.util.XmOption;

public class XMPtranslateLocalPragma {
  private XMPglobalDecl		_globalDecl;
  private boolean		_all_profile = false;
  private boolean		_selective_profile = false;
  private boolean		doScalasca = false;
  private boolean		doTlog = false;
  private XobjectDef		currentDef;

  public XMPtranslateLocalPragma(XMPglobalDecl globalDecl) {
    _globalDecl = globalDecl;
  }

  public void translate(FuncDefBlock def) {
    FunctionBlock fb = def.getBlock();
    currentDef = def.getDef();

    BlockIterator i = new topdownBlockIterator(fb);
    for (i.init(); !i.end(); i.next()) {
      Block b = i.getBlock();
      if (b.Opcode() ==  Xcode.XMP_PRAGMA) {
        PragmaBlock pb = (PragmaBlock)b;

        try {
	  translatePragma(pb);
        } catch (XMPexception e) {
          XMP.error(pb.getLineNo(), e.getMessage());
        }
      }
    }

    def.Finalize();
  }

  private void translatePragma(PragmaBlock pb) throws XMPexception {
    String pragmaName = pb.getPragma();

    switch (XMPpragma.valueOf(pragmaName)) {
      case NODES:
        { translateNodes(pb);			break; }
      case TEMPLATE:
        { translateTemplate(pb);		break; }
      case DISTRIBUTE:
        { translateDistribute(pb);		break; }
      case ALIGN:
        { translateAlign(pb);			break; }
      case SHADOW:
        { translateShadow(pb);			break; }
      case TASK:
        { translateTask(pb);			break; }
      case TASKS:
        { translateTasks(pb);			break; }
      case LOOP:
        { translateLoop(pb);			break; }
      case REFLECT:
        { translateReflect(pb);			break; }
      case BARRIER:
        { translateBarrier(pb);			break; }
      case REDUCTION:
        { translateReduction(pb);		break; }
      case BCAST:
        { translateBcast(pb);			break; }
      case GMOVE:
        { translateGmove(pb);			break; }
      case SYNC_MEMORY:
        { translateSyncMemory(pb);		break; }
      case SYNC_ALL:
        { translateSyncAll(pb);                 break; }
      case POST:
        { translatePost(pb);                    break; }
      case WAIT:
        { translateWait(pb);                    break; }
      case LOCAL_ALIAS:
        { translateLocalAlias(pb);		break; }
      case GPU_REPLICATE:
        { translateGpuData(pb);			break; }
      case GPU_REPLICATE_SYNC:
        { translateGpuSync(pb);			break; }
      case GPU_REFLECT:
        { translateGpuReflect(pb);		break; }
      case GPU_BARRIER:
        { break; }
      case GPU_LOOP:
        { translateGpuLoop(pb);			break; }
      default:
        throw new XMPexception("'" + pragmaName.toLowerCase() + "' directive is not supported yet");
    }
  }

  private void translateNodes(PragmaBlock pb) throws XMPexception {
    checkDeclPragmaLocation(pb);

    XobjList nodesDecl = (XobjList)pb.getClauses();
    XobjList nodesNameList = (XobjList)nodesDecl.getArg(0);
    XobjList nodesDeclCopy = (XobjList)nodesDecl.copy();

    Iterator<Xobject> iter = nodesNameList.iterator();
    while (iter.hasNext()) {
      Xobject x = iter.next();
      nodesDeclCopy.setArg(0, x);
      XMPnodes.translateNodes(nodesDeclCopy, _globalDecl, true, pb);
    }
  }

  private void translateTemplate(PragmaBlock pb) throws XMPexception {
    checkDeclPragmaLocation(pb);

    XobjList templateDecl = (XobjList)pb.getClauses();
    XobjList templateNameList = (XobjList)templateDecl.getArg(0);
    XobjList templateDeclCopy = (XobjList)templateDecl.copy();

    Iterator<Xobject> iter = templateNameList.iterator();
    while (iter.hasNext()) {
      Xobject x = iter.next();
      templateDeclCopy.setArg(0, x);
      XMPtemplate.translateTemplate(templateDeclCopy, _globalDecl, true, pb);
    }
  }

  private void translateDistribute(PragmaBlock pb) throws XMPexception {
    checkDeclPragmaLocation(pb);

    XobjList distributeDecl = (XobjList)pb.getClauses();
    XobjList distributeNameList = (XobjList)distributeDecl.getArg(0);
    XobjList distributeDeclCopy = (XobjList)distributeDecl.copy();

    Iterator<Xobject> iter = distributeNameList.iterator();
    while (iter.hasNext()) {
      Xobject x = iter.next();
      distributeDeclCopy.setArg(0, x);
      XMPtemplate.translateDistribute(distributeDeclCopy, _globalDecl, true, pb);
    }
  }

  private void translateAlign(PragmaBlock pb) throws XMPexception {
    checkDeclPragmaLocation(pb);

    XobjList alignDecl = (XobjList)pb.getClauses();
    XobjList alignNameList = (XobjList)alignDecl.getArg(0);
    XobjList alignDeclCopy = (XobjList)alignDecl.copy();

    Iterator<Xobject> iter = alignNameList.iterator();
    while (iter.hasNext()) {
      Xobject x = iter.next();
      alignDeclCopy.setArg(0, x);
      XMPalignedArray.translateAlign(alignDeclCopy, _globalDecl, true, pb);
    }
  }

  private void translateSyncMemory(PragmaBlock pb) throws XMPexception {
    pb.replace(_globalDecl.createFuncCallBlock("_XMP_coarray_sync_memory", null));
  }

  private void translateSyncAll(PragmaBlock pb) throws XMPexception {
    pb.replace(_globalDecl.createFuncCallBlock("_XMP_coarray_sync_all", null));
  }

  private void translatePost(PragmaBlock pb) throws XMPexception {
    checkDeclPragmaLocation(pb);
    XobjList postDecl = (XobjList)pb.getClauses();
    XobjList postNode = (XobjList)postDecl.getArg(0);
    Xobject  tag      = postDecl.getArg(1);

    Xobject postNodeNum = (Xobject)postNode.getArg(1);

    pb.replace(_globalDecl.createFuncCallBlock("_XMP_post", Xcons.List(postNodeNum, tag))); 
  }

  private void translateWait(PragmaBlock pb) throws XMPexception {
    checkDeclPragmaLocation(pb);
    XobjList waitDecl = (XobjList)pb.getClauses();
    Xobject numOfArgs = waitDecl.getArg(0); 
    XobjList args = null;
    Xobject waitNode = null;
    Xobject tag = null;

    switch (numOfArgs.getInt()) {
    case 0:  // no arguments
      args = Xcons.List(numOfArgs);
      break;
    case 1:  // only node
      waitNode = (Xobject)waitDecl.getArg(1).getArg(1);
      args = Xcons.List(numOfArgs, waitNode);
      break;
    case 2:
      waitNode = (Xobject)waitDecl.getArg(1).getArg(1);
      tag = waitDecl.getArg(2);
      args = Xcons.List(numOfArgs, waitNode, tag);
      break;
    }

    pb.replace(_globalDecl.createFuncCallBlock("_XMP_wait", args));
  }

  private void translateLocalAlias(PragmaBlock pb) throws XMPexception {
    checkDeclPragmaLocation(pb);
    XMPalignedArray.translateLocalAlias((XobjList)pb.getClauses(), _globalDecl, true, pb);
  }

  private void translateShadow(PragmaBlock pb) throws XMPexception {
    checkDeclPragmaLocation(pb);

    XobjList shadowDecl = (XobjList)pb.getClauses();
    XobjList shadowNameList = (XobjList)shadowDecl.getArg(0);
    XobjList shadowDeclCopy = (XobjList)shadowDecl.copy();

    Iterator<Xobject> iter = shadowNameList.iterator();
    while (iter.hasNext()) {
      Xobject x = iter.next();
      shadowDeclCopy.setArg(0, x);
      XMPshadow.translateShadow(shadowDeclCopy, _globalDecl, true, pb);
    }
  }

  private void translateReflect(PragmaBlock pb) throws XMPexception {
    Block reflectFuncCallBlock = XMPshadow.translateReflect(pb, _globalDecl);
    // add function calls for profiling            
    Xobject profileClause = pb.getClauses().getArg(1);
    if( _all_profile || (profileClause != null && _selective_profile)){
        if (doScalasca == true) {
            XobjList profileFuncArgs = Xcons.List(Xcons.StringConstant("#xmp reflect:" + pb.getLineNo()));
            reflectFuncCallBlock.insert(createScalascaStartProfileCall(profileFuncArgs));
            reflectFuncCallBlock.add(createScalascaEndProfileCall(profileFuncArgs));
        } else if (doTlog == true) {
            reflectFuncCallBlock.insert(createTlogMacroInvoke("_XMP_M_TLOG_REFLECT_IN", null));
            reflectFuncCallBlock.add(createTlogMacroInvoke("_XMP_M_TLOG_REFLECT_OUT", null));
        }
    } else if(profileClause == null && _selective_profile && doTlog == false){
        XobjList profileFuncArgs = null;
        reflectFuncCallBlock.insert(createScalascaProfileOffCall(profileFuncArgs));
        reflectFuncCallBlock.add(createScalascaProfileOnfCall(profileFuncArgs));
    }
  }

  private void translateGpuData(PragmaBlock pb) throws XMPexception {
    XMPgpuData.translateGpuData(pb, _globalDecl);
  }

  private void translateGpuSync(PragmaBlock pb) throws XMPexception {
    XMPgpuData.translateGpuSync(pb, _globalDecl);
  }

  private void translateGpuReflect(PragmaBlock pb) throws XMPexception {
    if (!XmOption.isXcalableMPGPU()) {
      XMP.warning("use -enable-gpu option to enable 'acc relfect' directive");
      translateReflect(pb);
    } else {
      XMPshadow.translateGpuReflect(pb, _globalDecl);
    }
  }

  private void translateGpuLoop(PragmaBlock pb) throws XMPexception {
    XobjList loopDecl = (XobjList)pb.getClauses();
    BlockList loopBody = pb.getBody();

    if (!XmOption.isXcalableMPGPU()) {
      XMP.warning("use -enable-gpu option to use 'acc loop' directive");
      pb.replace(Bcons.COMPOUND(loopBody));
      return;
    }

    // get block to schedule
    CforBlock schedBaseBlock = getOutermostLoopBlock(loopBody);

    // analyze loop
    if (loopDecl.getArg(0) == null) {
      loopDecl.setArg(0, Xcons.List(Xcons.String(schedBaseBlock.getInductionVar().getName())));
      analyzeFollowingGpuLoop(pb, schedBaseBlock);
    } else {
      analyzeMultipleGpuLoop(pb, schedBaseBlock);
    }

    // translate
    // FIXME implement reduction
    Block newLoopBlock = translateGpuClause(pb, null, schedBaseBlock);
    schedBaseBlock.replace(newLoopBlock);

    // replace pragma
    Block loopFuncCallBlock = Bcons.COMPOUND(loopBody);
    pb.replace(loopFuncCallBlock);
  }

  private void analyzeFollowingGpuLoop(PragmaBlock pb, CforBlock schedBaseBlock) throws XMPexception {
    // schedule loop
    analyzeGpuLoop(pb, schedBaseBlock, schedBaseBlock);
  }

  private void analyzeMultipleGpuLoop(PragmaBlock pb, CforBlock schedBaseBlock) throws XMPexception {
    // start translation
    XobjList loopDecl = (XobjList)pb.getClauses();
    BlockList loopBody = pb.getBody();

    // iterate index variable list
    XobjList loopVarList = (XobjList)loopDecl.getArg(0);
    Vector<CforBlock> loopVector = new Vector<CforBlock>(XMPutil.countElmts(loopVarList));
    for (XobjArgs i = loopVarList.getArgs(); i != null; i = i.nextArgs()) {
      loopVector.add(findLoopBlock(loopBody, i.getArg().getString()));
    }

    // schedule loop
    Iterator<CforBlock> it = loopVector.iterator();
    while (it.hasNext()) {
      CforBlock forBlock = it.next();
      analyzeGpuLoop(pb, forBlock, schedBaseBlock);
    }
  }

  private void analyzeGpuLoop(PragmaBlock pb, CforBlock forBlock, CforBlock schedBaseBlock) throws XMPexception {
    Xobject loopIndex = forBlock.getInductionVar();
    String loopIndexName = loopIndex.getSym();

    Ident initId = declIdentWithBlock(schedBaseBlock,
                                      "_XMP_loop_init_" + loopIndexName, Xtype.intType);
    Ident condId = declIdentWithBlock(schedBaseBlock,
                                      "_XMP_loop_cond_" + loopIndexName, Xtype.intType);
    Ident stepId = declIdentWithBlock(schedBaseBlock,
                                      "_XMP_loop_step_" + loopIndexName, Xtype.intType);

    schedBaseBlock.insert(Xcons.Set(initId.Ref(), forBlock.getLowerBound()));
    schedBaseBlock.insert(Xcons.Set(condId.Ref(), forBlock.getUpperBound()));
    schedBaseBlock.insert(Xcons.Set(stepId.Ref(), forBlock.getStep()));

    XMPutil.putLoopIter(schedBaseBlock, loopIndexName, Xcons.List(initId, condId, stepId));
  }

  private void translateTask(PragmaBlock pb) throws XMPexception {
    // start translation
    XobjList taskDecl = (XobjList)pb.getClauses();
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);
    BlockList taskBody = pb.getBody();

    // create function arguments
    XobjList onRef = (XobjList)taskDecl.getArg(0);
    XMPquadruplet<String, Boolean, XobjList, XMPobject> execOnRefArgs = createExecOnRefArgs(onRef, localXMPsymbolTable);
    String execFuncSurfix = execOnRefArgs.getFirst();
    XobjList execFuncArgs = execOnRefArgs.getThird();
    XMPobject onRefObject = execOnRefArgs.getForth();

    // setup task finalizer
    Ident finFuncId = _globalDecl.declExternFunc("_XMP_pop_nodes");
    setupFinalizer(taskBody, finFuncId, null);

    // create function call
    BlockList taskFuncCallBlockList = Bcons.emptyBody();
    Ident taskDescId = taskBody.declLocalIdent("_XMP_TASK_desc", Xtype.voidPtrType, StorageClass.AUTO,
					       Xcons.Cast(Xtype.voidPtrType, Xcons.IntConstant(0)));

    execFuncArgs.cons(taskDescId.getAddr());
    Ident execFuncId = execFuncId = _globalDecl.declExternFunc("_XMP_exec_task_" + execFuncSurfix, Xtype.intType);
    Block taskFuncCallBlock = Bcons.IF(BasicBlock.Cond(execFuncId.Call(execFuncArgs)), taskBody, null);
    taskFuncCallBlockList.add(taskFuncCallBlock);
    pb.replace(Bcons.COMPOUND(taskFuncCallBlockList));

    XobjList arg = Xcons.List(Xcode.POINTER_REF, taskDescId.Ref());
    taskBody.add(_globalDecl.createFuncCallBlock("_XMP_exec_task_NODES_FINALIZE", arg));

    // add function calls for profiling                                                              
    Xobject profileClause = taskDecl.getArg(1);
    if( _all_profile || (profileClause != null && _selective_profile)){
        if (doScalasca == true) {
            XobjList profileFuncArgs = Xcons.List(Xcons.StringConstant("#xmp task:" + pb.getLineNo()));
            taskFuncCallBlock.insert(createScalascaStartProfileCall(profileFuncArgs));
            taskFuncCallBlock.add(createScalascaEndProfileCall(profileFuncArgs));
        } else if (doTlog == true) {
            taskFuncCallBlock.insert(createTlogMacroInvoke("_XMP_M_TLOG_TASK_IN", null));
            taskFuncCallBlock.add(createTlogMacroInvoke("_XMP_M_TLOG_TASK_OUT", null));
        }
    } else if(profileClause == null && _selective_profile && doTlog == false){
        XobjList profileFuncArgs = null;
        taskFuncCallBlock.insert(createScalascaProfileOffCall(profileFuncArgs));
        taskFuncCallBlock.add(createScalascaProfileOnfCall(profileFuncArgs));
    }
  }

  private void translateTasks(PragmaBlock pb) {
    System.out.println("TASKS:" + pb.toXobject().toString());
  }

  private void translateLoop(PragmaBlock pb) throws XMPexception {
    XobjList loopDecl = (XobjList)pb.getClauses();
    BlockList loopBody = pb.getBody();

    // get block to schedule
    CforBlock schedBaseBlock = getOutermostLoopBlock(loopBody);

    // schedule loop
    XobjList loopIterList = (XobjList)loopDecl.getArg(0);
    if (loopIterList == null) {
      loopIterList = Xcons.List(Xcons.String(schedBaseBlock.getInductionVar().getName()));
      loopDecl.setArg(0, loopIterList);
      translateFollowingLoop(pb, schedBaseBlock);
    } else {
      translateMultipleLoop(pb, schedBaseBlock);
    }

    // translate reduction clause
    XobjList reductionRefList = (XobjList)loopDecl.getArg(2);
    if (reductionRefList != null) {
      XobjList schedVarList = null;
      if (loopDecl.getArg(0) == null) {
        schedVarList = Xcons.List(Xcons.String(schedBaseBlock.getInductionVar().getSym()));
      } else {
        schedVarList = (XobjList)loopDecl.getArg(0).copy();
      }

      BlockList reductionBody = createReductionClauseBody(pb, reductionRefList, schedBaseBlock);
      schedBaseBlock.add(createReductionClauseBlock(pb, reductionBody, schedVarList));
    }

    // translate multicore clause
    XobjList multicoreClause = (XobjList)loopDecl.getArg(3);
    if (multicoreClause != null) {
      String devName = multicoreClause.getArg(0).getString();

      if (devName.equals("acc")) {
        if (XmOption.isXcalableMPGPU()) {
          Block newLoopBlock = translateGpuClause(pb, reductionRefList, schedBaseBlock);
          schedBaseBlock.replace(newLoopBlock);
        } else {
          XMP.warning("use '-enable-gpu' compiler option to use gpu clause");
        }
      } else if (devName.equals("threads")) {
        if (XmOption.isXcalableMPthreads()) {
          XobjList devArgs = (XobjList)multicoreClause.getArg(1);
          Block newLoopBlock = translateThreadsClauseToOMPpragma(devArgs, reductionRefList, schedBaseBlock, loopIterList);
          schedBaseBlock.replace(newLoopBlock);
        } else {
          XMP.warning("use '-enable-threads' compiler option to use 'threads' clause");
        }
      } else {
        throw new XMPexception("unknown clause in loop directive");
      }
    }

    // rewrite array refs in loop
    topdownXobjectIterator iter = new topdownXobjectIterator(getLoopBody(schedBaseBlock).toXobject());
    for (iter.init(); !iter.end(); iter.next()) {
	XMPrewriteExpr.rewriteArrayRefInLoop(iter.getXobject(), _globalDecl, XMPlocalDecl.getXMPsymbolTable(pb));
    }

    // replace pragma
    Block loopFuncCallBlock = Bcons.COMPOUND(loopBody);
    pb.replace(loopFuncCallBlock);

    // add function calls for profiling                                                              
    Xobject profileClause = loopDecl.getArg(4);
    if( _all_profile || (profileClause != null && _selective_profile)){
        if (doScalasca == true) {
            XobjList profileFuncArgs = Xcons.List(Xcons.StringConstant("#xmp loop:" + pb.getLineNo()));
            loopFuncCallBlock.insert(createScalascaStartProfileCall(profileFuncArgs));
            loopFuncCallBlock.add(createScalascaEndProfileCall(profileFuncArgs));
        } else if (doTlog == true) {
            loopFuncCallBlock.insert(createTlogMacroInvoke("_XMP_M_TLOG_LOOP_IN", null));
            loopFuncCallBlock.add(createTlogMacroInvoke("_XMP_M_TLOG_LOOP_OUT", null));
        }
    } else if(profileClause == null && _selective_profile && doTlog == false){
        XobjList profileFuncArgs = null;
        loopFuncCallBlock.insert(createScalascaProfileOffCall(profileFuncArgs));
        loopFuncCallBlock.add(createScalascaProfileOnfCall(profileFuncArgs));
    }
  }

  // XXX only supports C language
  private Block translateGpuClause(PragmaBlock pb, XobjList reductionRefList,
                                   CforBlock loopBlock) throws XMPexception {
    Ident funcId = _globalDecl.declExternIdent(_globalDecl.genSym(XMP.GPU_FUNC_PREFIX),
                                               Xtype.Function(Xtype.voidType));

    XobjList funcArgs = setupGPUparallelFunc(funcId, loopBlock, pb);

    return _globalDecl.createFuncCallBlock(funcId.getName(), funcArgs);
  }

  private XobjList setupGPUparallelFunc(Ident funcId, CforBlock loopBlock, PragmaBlock pb) throws XMPexception {
    XobjList loopVarList = (XobjList)((XobjList)(pb.getClauses())).getArg(0);
    XobjList newLoopVarList = setupGpuLoopBlock(Xcons.List(loopBlock.findVarIdent(loopBlock.getInductionVar().getSym())),
                                                loopVarList, loopBlock.getBody().getHead());

    // get params
    XobjList paramIdList = getGPUfuncParams(loopBlock, pb);

    // setup & decompile GPU function body
    XMPgpuDecompiler.decompile(funcId, paramIdList, getXMPalignedArrays(loopBlock),
                               loopBlock, newLoopVarList, pb, _globalDecl.getEnv());

    // generate func args
    XobjList funcArgs = Xcons.List();
    for (XobjArgs i = paramIdList.getArgs(); i != null; i = i.nextArgs()) {
      Ident paramId = (Ident)i.getArg();
      String paramName = paramId.getName();
      XMPgpuData gpuData = XMPgpuDataTable.findXMPgpuData(paramId.getName(), loopBlock);
      if (gpuData == null) {
        if (paramId.Type().isArray()) {
          throw new XMPexception("array '" + paramName + "' should be declared as a gpuData");
        } else {
          funcArgs.add(paramId.Ref());
        }
      } else {
        XMPalignedArray alignedArray = gpuData.getXMPalignedArray();
        funcArgs.add(gpuData.getDeviceAddrId().Ref());
      }
    }

    return funcArgs;
  }

  private XobjList setupGpuLoopBlock(XobjList newLoopVarList, XobjList loopVarList, Block b) throws XMPexception {
    switch (b.Opcode()) {
      case FOR_STATEMENT:
        {
          CforBlock loopBlock = (CforBlock)b;
          if (!loopBlock.isCanonical()) {
            loopBlock.Canonicalize();
          }

          Block bodyBlock = b.getBody().getHead();
          String loopVarName = loopBlock.getInductionVar().getSym();
          if (XMPutil.hasElmt(loopVarList, loopVarName)) {
            newLoopVarList.insert(b.findVarIdent(loopVarName));
            b.getParentBlock().setBody(loopBlock.getBody());
          }

          return setupGpuLoopBlock(newLoopVarList, loopVarList, bodyBlock);
        }
      case COMPOUND_STATEMENT:
        return setupGpuLoopBlock(newLoopVarList, loopVarList, b.getBody().getHead());
      default:
        return newLoopVarList;
    }
  }

  private ArrayList<XMPalignedArray> getXMPalignedArrays(CforBlock loopBlock) throws XMPexception {
    ArrayList<XMPalignedArray> alignedArrayList = new ArrayList<XMPalignedArray>();
    XobjList arrayNameList = Xcons.List();

    BasicBlockExprIterator iter = new BasicBlockExprIterator(loopBlock.getBody());
    for (iter.init(); !iter.end(); iter.next()) {
      Xobject expr = iter.getExpr();
      topdownXobjectIterator exprIter = new topdownXobjectIterator(expr);
      for (exprIter.init(); !exprIter.end(); exprIter.next()) {
        Xobject x = exprIter.getXobject();
        switch (x.Opcode()) {
          case ARRAY_REF:
            {
              String varName = x.getArg(0).getSym();
              if (!XMPutil.hasElmt(arrayNameList, varName)) {
                arrayNameList.add(Xcons.String(varName));

                XMPgpuData gpuData = XMPgpuDataTable.findXMPgpuData(varName, loopBlock);
                if (gpuData == null) {
                  throw new XMPexception("array '" + varName + "' shoud be declared as a gpuData");
                } else {
                  XMPalignedArray alignedArray = gpuData.getXMPalignedArray();
                  if (alignedArray != null) {
                    alignedArrayList.add(alignedArray);
                  }
                }
              }
            } break;
          default:
        }
      }
    }

    return alignedArrayList;
  }

  private XobjList getGPUfuncParams(CforBlock loopBlock, PragmaBlock pb) throws XMPexception {
    XobjList params = Xcons.List();
    XobjList loopVars = Xcons.List();

    BasicBlockExprIterator iter = new BasicBlockExprIterator(loopBlock.getBody());
    for (iter.init(); !iter.end(); iter.next()) {
      Xobject expr = iter.getExpr();
      topdownXobjectIterator exprIter = new topdownXobjectIterator(expr);
      for (exprIter.init(); !exprIter.end(); exprIter.next()) {
        Xobject x = exprIter.getXobject();
        switch (x.Opcode()) {
          case VAR:
            {
              String varName = x.getName();
              if (!(XMPutil.hasIdent(params, varName) ||
                    XMPutil.hasElmt(loopVars, varName))) {
                XobjList loopIter = XMPutil.getLoopIter(loopBlock, varName);
                if (loopIter == null) {
                  Ident id = loopBlock.findVarIdent(varName);
                  if (id != null) {
                    params.add(Ident.Param(varName, id.Type()));
                  }
                } else {
                  loopVars.add(Xcons.String(varName));
                }
              }
            } break;
          case ARRAY_REF:
            {
              String varName = x.getArg(0).getSym();
              if (!XMPutil.hasIdent(params, varName)) {
                XMPgpuData gpuData = XMPgpuDataTable.findXMPgpuData(varName, loopBlock);
                if (gpuData == null) {
                  throw new XMPexception("array '" + varName + "' shoud be declared as a gpuData");
                } else {
                  XMPalignedArray alignedArray = gpuData.getXMPalignedArray();
                  if (alignedArray == null) {
                    Ident id = loopBlock.findVarIdent(varName);
                    params.add(Ident.Param(varName, id.Type()));
                  } else {
                    Xtype alignedArrayParamType = null;
                    if (alignedArray.realloc()) {
                      alignedArrayParamType = alignedArray.getAddrId().Type();
                    } else {
                      alignedArrayParamType = alignedArray.getArrayId().Type();
                    }

                    params.add(Ident.Param(varName, alignedArrayParamType));
                    params.add(Ident.Param(XMP.GPU_DEVICE_DESC_PREFIX_ + varName, Xtype.voidPtrType));
                  }
                }
              }
            } break;
          default:
        }
      }
    }

    // FIXME consider the order
    XobjList loopIndexList = (XobjList)pb.getClauses().getArg(0);
    for (Xobject loopIndex : loopIndexList) {
      XobjList loopIter = XMPutil.getLoopIter(loopBlock, loopIndex.getName()); 

      Ident initId = (Ident)loopIter.getArg(0);
      params.add(Ident.Param(initId.getName(), initId.Type()));

      Ident condId = (Ident)loopIter.getArg(1);
      params.add(Ident.Param(condId.getName(), condId.Type()));

      Ident stepId = (Ident)loopIter.getArg(2);
      params.add(Ident.Param(stepId.getName(), stepId.Type()));
    }

    return params;
  }

  private Block createOMPpragmaBlock(OMPpragma pragma, Xobject args, Block body) {
    return Bcons.PRAGMA(Xcode.OMP_PRAGMA, pragma.toString(), args, Bcons.blockList(body));
  }

  private Block translateThreadsClauseToOMPpragma(XobjList threadsClause, XobjList reductionRefList,
                                                  CforBlock loopBlock, XobjList loopIterList) throws XMPexception {
    Xobject parallelClause = Xcons.statementList();
    XobjList forClause = Xcons.List();

    if (threadsClause != null) {
      for (Xobject c : threadsClause) {
        OMPpragma p = OMPpragma.valueOf(c.getArg(0));
        switch (p) {
          case DATA_PRIVATE:
          case DATA_FIRSTPRIVATE:
          case DATA_LASTPRIVATE:
            {
              compile_THREADS_name_list(c.getArg(1));
              forClause.add(c);
            } break;
          case DIR_NUM_THREADS:
          case DIR_IF:
            parallelClause.add(c);
            break;
          default:
            throw new XMPexception("unknown threads clause");
        }
      }
    }

    // FIXME compare loopIterList with private, firstprivate var list
    if (loopIterList != null) {
      String schedLoopIterName = loopBlock.getInductionVar().getSym();
      XobjList privateList = Xcons.List();
      XobjList firstPrivateList = Xcons.List();
      Iterator<Xobject> iter = loopIterList.iterator();
      while (iter.hasNext()) {
        Xobject x = iter.next();
        String iterName = x.getName();
        if (!iterName.equals(schedLoopIterName)) {
          privateList.add(Xcons.Symbol(Xcode.IDENT, iterName));
          firstPrivateList.add(Xcons.Symbol(Xcode.IDENT, "_XMP_loop_init_" + iterName));
          firstPrivateList.add(Xcons.Symbol(Xcode.IDENT, "_XMP_loop_cond_" + iterName));
          firstPrivateList.add(Xcons.Symbol(Xcode.IDENT, "_XMP_loop_step_" + iterName));
        }
      }

      forClause.add(Xcons.List(Xcode.LIST, Xcons.String(OMPpragma.DATA_PRIVATE.toString()), privateList));
      forClause.add(Xcons.List(Xcode.LIST, Xcons.String(OMPpragma.DATA_FIRSTPRIVATE.toString()), firstPrivateList));
    }

    addReductionClauseToOMPclause(forClause, reductionRefList);

    return createOMPpragmaBlock(OMPpragma.PARALLEL, parallelClause,
                                createOMPpragmaBlock(OMPpragma.FOR, forClause,
                                                     loopBlock));
  }

  private void addReductionClauseToOMPclause(XobjList OMPclause, XobjList reductionRefList) throws XMPexception {
    if (reductionRefList == null) {
      return;
    }

    for (Xobject c : reductionRefList) {
      OMPpragma redOp = null;

      XobjInt reductionOp = (XobjInt)c.getArg(0);
      switch (reductionOp.getInt()) {
        case XMPcollective.REDUCE_SUM:
          redOp = OMPpragma.DATA_REDUCTION_PLUS;
          break;
        case XMPcollective.REDUCE_PROD:
          redOp = OMPpragma.DATA_REDUCTION_MUL;
          break;
        case XMPcollective.REDUCE_BAND:
          redOp = OMPpragma.DATA_REDUCTION_BITAND;
          break;
        case XMPcollective.REDUCE_LAND:
          redOp = OMPpragma.DATA_REDUCTION_LOGAND;
          break;
        case XMPcollective.REDUCE_BOR:
          redOp = OMPpragma.DATA_REDUCTION_BITOR;
          break;
        case XMPcollective.REDUCE_LOR:
          redOp = OMPpragma.DATA_REDUCTION_LOGOR;
          break;
        case XMPcollective.REDUCE_BXOR:
          redOp = OMPpragma.DATA_REDUCTION_BITXOR;
          break;
        case XMPcollective.REDUCE_LXOR:
        case XMPcollective.REDUCE_MAX:
        case XMPcollective.REDUCE_MIN:
        case XMPcollective.REDUCE_FIRSTMAX:
        case XMPcollective.REDUCE_FIRSTMIN:
        case XMPcollective.REDUCE_LASTMAX:
        case XMPcollective.REDUCE_LASTMIN:
          throw new XMPexception("the operation cannot be translated to OpenMP clause");
        default:
          throw new XMPexception("unknown reduction operation");
      }

      XobjList redVarList = Xcons.List();
      for (Xobject x : (XobjList)c.getArg(1)) {
        redVarList.add(Xcons.Symbol(Xcode.IDENT, x.getArg(0).getName()));
      }

      OMPclause.add(omp_pg_list(redOp, redVarList));
    }
  }

  private Xobject omp_pg_list(OMPpragma pg, Xobject args) {
    return Xcons.List(Xcode.LIST, Xcons.String(pg.toString()), args);
  }

  // XXX not implemented yet, check variables
  private void compile_THREADS_name_list(Xobject name_list) throws XMPexception {
    if (name_list == null) {
      return;
    }
  }

  private Block createReductionClauseBlock(PragmaBlock pb, BlockList reductionBody, XobjList schedVarList) throws XMPexception {
    XobjList loopDecl = (XobjList)pb.getClauses();
    XobjList onRef = (XobjList)loopDecl.getArg(1);
    String onRefObjName = onRef.getArg(0).getString();
    XobjList subscriptList = (XobjList)onRef.getArg(1);

    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);
    XMPobject onRefObj = _globalDecl.getXMPobject(onRefObjName, localXMPsymbolTable);
    if (onRefObj == null) {
      throw new XMPexception("cannot find '" + onRefObjName + "' nodes/template");
    }

    String initFuncSurfix = null;
    switch (onRefObj.getKind()) {
      case XMPobject.TEMPLATE:
        initFuncSurfix = "TEMPLATE";
        break;
      case XMPobject.NODES:
        initFuncSurfix = "NODES";
        break;
      default:
        throw new XMPexception("unknown object type");
    }

    // create function arguments
    XobjList initFuncArgs = Xcons.List(onRefObj.getDescId().Ref());

    boolean initComm = false;
    for (XobjArgs i = subscriptList.getArgs(); i != null; i = i.nextArgs()) {
      String subscript = i.getArg().getString();
      if (XMPutil.hasElmt(schedVarList, subscript)) {
        initFuncArgs.add(Xcons.Cast(Xtype.intType, Xcons.IntConstant(0)));
      } else {
        initComm = true;
        initFuncArgs.add(Xcons.Cast(Xtype.intType, Xcons.IntConstant(1)));
      }
    }

    if (initComm) {
      // setup finalizer
      Ident finFuncId = _globalDecl.declExternFunc("_XMP_pop_n_free_nodes");
      setupFinalizer(reductionBody, finFuncId, null);

      // create function call
      Ident initFuncId = _globalDecl.declExternFunc("_XMP_init_reduce_comm_" + initFuncSurfix, Xtype.intType);

      return Bcons.IF(BasicBlock.Cond(initFuncId.Call(initFuncArgs)), reductionBody, null);
    } else {
      return Bcons.COMPOUND(reductionBody);
    }
  }

  private void translateFollowingLoop(PragmaBlock pb, CforBlock schedBaseBlock) throws XMPexception {
    XobjList loopDecl = (XobjList)pb.getClauses();
    ArrayList iteraterList = new ArrayList();  // Not used 

    // schedule loop
    scheduleLoop(pb, schedBaseBlock, schedBaseBlock, iteraterList);
    insertScheduleIndexFunction(pb, schedBaseBlock, schedBaseBlock, iteraterList);
  }

  private void translateMultipleLoop(PragmaBlock pb, CforBlock schedBaseBlock) throws XMPexception {
    // start translation
    XobjList loopDecl = (XobjList)pb.getClauses();
    BlockList loopBody = pb.getBody();

    // iterate index variable list
    XobjList loopVarList = (XobjList)loopDecl.getArg(0);
    Vector<CforBlock> loopVector = new Vector<CforBlock>(XMPutil.countElmts(loopVarList));
    for (XobjArgs i = loopVarList.getArgs(); i != null; i = i.nextArgs()) {
      loopVector.add(findLoopBlock(loopBody, i.getArg().getString()));
    }

    // schedule loop
    Iterator<CforBlock> it = loopVector.iterator();
    ArrayList iteraterList = new ArrayList();
    while (it.hasNext()) {
      CforBlock forBlock = it.next();
      scheduleLoop(pb, forBlock, schedBaseBlock, iteraterList);
    }

    it = loopVector.iterator();
    while (it.hasNext()) {
      CforBlock forBlock = it.next();
      insertScheduleIndexFunction(pb, forBlock, schedBaseBlock, iteraterList);
    }
  }

  private void insertScheduleIndexFunction(PragmaBlock pb, CforBlock forBlock, CforBlock schedBaseBlock, 
					   ArrayList iteraterList) throws XMPexception {
    XobjList loopDecl = (XobjList)pb.getClauses();
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(schedBaseBlock);
    Xobject onRef = loopDecl.getArg(1);
    String onRefObjName = onRef.getArg(0).getString();
    XMPobject onRefObj = _globalDecl.getXMPobject(onRefObjName, localXMPsymbolTable);

    XMPtemplate templateObj = (XMPtemplate)onRefObj;
    XobjList templateSubscriptList = (XobjList)onRef.getArg(1);
    Xobject loopIndex = forBlock.getInductionVar();
    String loopIndexName = loopIndex.getSym();
    XobjList funcArgs = Xcons.List();
    Xobject lb = forBlock.getLowerBound();
    Xobject up = forBlock.getUpperBound();
    Xobject step = forBlock.getStep();
    funcArgs.add(lb);
    funcArgs.add(up);
    funcArgs.add(step);

    Ident parallelInitId = declIdentWithBlock(schedBaseBlock, "_XMP_loop_init_" + loopIndexName, Xtype.intType);
    Ident parallelCondId = declIdentWithBlock(schedBaseBlock, "_XMP_loop_cond_" + loopIndexName, Xtype.intType);
    Ident parallelStepId = declIdentWithBlock(schedBaseBlock, "_XMP_loop_step_" + loopIndexName, Xtype.intType);
    
    funcArgs.add(parallelInitId.getAddr());
    funcArgs.add(parallelCondId.getAddr());
    funcArgs.add(parallelStepId.getAddr());

    XobjInt templateIndexArg = null;
    int templateIndex = 0;
    int distManner = 0;
    String distMannerString = null;
    for (XobjArgs i = templateSubscriptList.getArgs(); i != null; i = i.nextArgs()) {
      String s = i.getArg().getString();
      if (s.equals(loopIndexName)) {
        templateIndexArg = Xcons.IntConstant(templateIndex);
        distManner = templateObj.getDistMannerAt(templateIndex);
        distMannerString = templateObj.getDistMannerStringAt(templateIndex);
      }
      templateIndex++;
    }
    funcArgs.add(templateObj.getDescId().Ref());
    funcArgs.add(templateIndexArg);
    
    Ident funcId = _globalDecl.declExternFunc("_XMP_sched_loop_template_" + distMannerString);
    
    int[] position = {iteraterList.size()};
    boolean[] flag = {false, false, false};
    String[] insertedIteraterList = new String[3];
    flag[0] = getPositionInsertScheFuncion(lb,   0, position, iteraterList, insertedIteraterList);
    flag[1] = getPositionInsertScheFuncion(up,   1, position, iteraterList, insertedIteraterList);
    flag[2] = getPositionInsertScheFuncion(step, 2, position, iteraterList, insertedIteraterList);

    if(position[0] == iteraterList.size()){
      schedBaseBlock.insert(funcId.Call(funcArgs));
    }
    else{
      if(flag[0]){ 
	insertScheFuncion(lb,   flag, position[0], 0, schedBaseBlock, funcArgs, templateObj, 
			  funcId, iteraterList, insertedIteraterList);
      }
      if(flag[1]){
	insertScheFuncion(up,   flag, position[0], 1, schedBaseBlock, funcArgs, templateObj, 
			  funcId, iteraterList, insertedIteraterList);
      }
      if(flag[2]){
	insertScheFuncion(step, flag, position[0], 2, schedBaseBlock, funcArgs, templateObj,
			  funcId, iteraterList, insertedIteraterList);
      }
    }
  }

  private void insertScheFuncion(Xobject v, boolean[] flag, int position, int num, CforBlock schedBaseBlock, XobjList funcArgs, 
				 XMPtemplate templateObj, Ident funcId, ArrayList iteraterList, String[] insertedIteraterList) throws XMPexception {

    Block b = schedBaseBlock;
    for(int i=0;i<iteraterList.size()-position;i++){
      b = b.getBody().getHead();
    }

    Xobject func = funcId.Call(funcArgs);
    int loop_depth = 0;
    String index = insertedIteraterList[num];
    for(int i=0;i<iteraterList.size();i++){
      if(iteraterList.get(i).toString().equals(index)){
	loop_depth = i;
      }
    }
    v = XMPrewriteExpr.calcLtoG(templateObj, loop_depth, v); 
    funcArgs.setArg(num, v);

    // Insert Function
    if(num == 0){
      b.insert(func);
      return;
    }

    if(num == 1){
      if(flag[0]){
	return; 
      }
      else{
        b.insert(func);
      }
      return;
    }

    if(num == 2){
      if(flag[0] || flag[1]){
	return;
      }
      else{
	b.insert(func);
      }
      return;
    }
  }

  private boolean getPositionInsertScheFuncion(Xobject v, int num, int[] position, ArrayList iteraterList, 
					       String[] insertedIteraterList) throws XMPexception {
    if(v.Opcode() != Xcode.INT_CONSTANT){
      XobjList vList = null;
      if(v.isVariable()){
        vList = Xcons.List(v);
      }
      else{
        vList = XobjArgs2XobjList(v, Xcons.List());
      }
      for (XobjArgs i = vList.getArgs(); i != null; i = i.nextArgs()) {
	if(i.getArg().isVariable()){
          for(int j=0;j<iteraterList.size();j++){
            if(i.getArg().getSym().equals(iteraterList.get(j))){
	      if(position[0] > j){
		position[0] = j;
	      }
	      insertedIteraterList[num] = iteraterList.get(j).toString();
	      return true;
            }
	  }
	}
      }
    }
    return false;
  }

  private XobjList XobjArgs2XobjList(Xobject x, XobjList a){
    for (XobjArgs i = x.getArgs(); i != null; i = i.nextArgs()) {

      if(i.getArg().isBinaryOp()){
	XobjArgs2XobjList(i.getArg(), a);
      } 
      else{
	a.add(i.getArg());
      }
    }
    return a;
  }

  private BlockList createReductionClauseBody(PragmaBlock pb, XobjList reductionRefList,
                                              CforBlock schedBaseBlock) throws XMPexception {
    // create init block
    Ident getRankFuncId = _globalDecl.declExternFunc("_XMP_get_execution_nodes_rank", Xtype.intType);
    IfBlock reductionInitIfBlock = (IfBlock)Bcons.IF(BasicBlock.Cond(Xcons.binaryOp(Xcode.LOG_NEQ_EXPR, getRankFuncId.Call(null),
                                                                                                        Xcons.IntConstant(0))),
                                                     null, null);

    // create function call
    Iterator<Xobject> it = reductionRefList.iterator();
    BlockList reductionBody = Bcons.emptyBody();
    while (it.hasNext()) {
      XobjList reductionRef = (XobjList)it.next();
      Vector<XobjList> reductionFuncArgsList = createReductionArgsList(reductionRef, pb,
                                                                       true, schedBaseBlock, reductionInitIfBlock);
      String reductionFuncType = createReductionFuncType(reductionRef, pb);

      reductionBody.add(createReductionFuncCallBlock(false, reductionFuncType + "_CLAUSE",
                                                     null, reductionFuncArgsList));
    }

    if (reductionInitIfBlock.getThenBody() != null) {
      schedBaseBlock.insert(reductionInitIfBlock);
    }

    return reductionBody;
  }

  private void scheduleLoop(PragmaBlock pb, CforBlock forBlock, CforBlock schedBaseBlock, ArrayList iteraterList) throws XMPexception {
    XobjList loopDecl = (XobjList)pb.getClauses();
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(schedBaseBlock);

    // analyze <on-ref>
    Xobject onRef = loopDecl.getArg(1);
    String onRefObjName = onRef.getArg(0).getString();
    XMPobject onRefObj = _globalDecl.getXMPobject(onRefObjName, localXMPsymbolTable);
    if (onRefObj == null) {
      throw new XMPexception("cannot find '" + onRefObjName + "' nodes/template");
    }

    switch (onRefObj.getKind()) {
      case XMPobject.TEMPLATE:
        {
          XMPtemplate onRefTemplate = (XMPtemplate)onRefObj;
          if (!onRefTemplate.isFixed()) {
            throw new XMPexception("template '" + onRefObjName + "' is not fixed");
          }

          if (!onRefTemplate.isDistributed()) {
            throw new XMPexception("template '" + onRefObjName + "' is not distributed");
          }

          callLoopSchedFuncTemplate(onRefTemplate, (XobjList)onRef.getArg(1), forBlock, schedBaseBlock, iteraterList);
        } break;
      case XMPobject.NODES:
        callLoopSchedFuncNodes((XMPnodes)onRefObj, (XobjList)onRef.getArg(1), forBlock, schedBaseBlock);
        break;
      default:
        throw new XMPexception("unknown object type");
    }
  }

  private static CforBlock getOutermostLoopBlock(BlockList body) throws XMPexception {
    Block b = body.getHead();
    if (b != null) {
      if (b.Opcode() == Xcode.FOR_STATEMENT) {
        LineNo blockLnObj = b.getLineNo();

        // XXX too strict?
        if (b.getNext() != null) {
          throw new XMPexception("only one loop statement is allowed in loop directive");
        }

        CforBlock forBlock = (CforBlock)b;
        forBlock.Canonicalize();
        if (forBlock.isCanonical()) {
          return forBlock;
        } else {
          throw new XMPexception("loop statement is not canonical");
        }
      }
      else if (b.Opcode() == Xcode.COMPOUND_STATEMENT) {
        return getOutermostLoopBlock(b.getBody());
      }
    }

    throw new XMPexception("cannot find the loop statement");
  }

  private static BlockList getLoopBody(Block b) throws XMPexception {
    return b.getBody();
  }

  private static CforBlock findLoopBlock(BlockList body, String loopVarName) throws XMPexception {
    Block b = body.getHead();
    if (b != null) {
      switch (b.Opcode()) {
        case FOR_STATEMENT:
          {
            CforBlock forBlock = (CforBlock)b;
            forBlock.Canonicalize();
            if (!(forBlock.isCanonical())) {
              throw new XMPexception("loop is not canonical");
            }

            if (forBlock.getInductionVar().getSym().equals(loopVarName)) {
              return (CforBlock)b;
            } else {
              return findLoopBlock(forBlock.getBody(), loopVarName);
            }
          }
        case COMPOUND_STATEMENT:
          return findLoopBlock(b.getBody(), loopVarName);
        case XMP_PRAGMA:
        case OMP_PRAGMA:
          throw new XMPexception("reached to a openmp/xcalablemp directive");
      }
    }

    throw new XMPexception("cannot find the loop statement");
  }

  private void callLoopSchedFuncTemplate(XMPtemplate templateObj, XobjList templateSubscriptList,
                                         CforBlock forBlock, CforBlock schedBaseBlock, ArrayList iteraterList) throws XMPexception {
    Xobject loopIndex = forBlock.getInductionVar();
    String loopIndexName = loopIndex.getSym();
    iteraterList.add(loopIndexName);

    int templateIndex = 0;
    int templateDim = templateObj.getDim();
    XobjInt templateIndexArg = null;
    int distManner = 0;
    String distMannerString = null;
    for (XobjArgs i = templateSubscriptList.getArgs(); i != null; i = i.nextArgs()) {
      if (templateIndex >= templateDim) {
        throw new XMPexception("wrong template dimensions, too many");
      }

      String s = i.getArg().getString();
      if (s.equals(loopIndexName)) {
        if (templateIndexArg != null) {
          throw new XMPexception("loop index '" + loopIndexName + "' is already described");
        }

        templateIndexArg = Xcons.IntConstant(templateIndex);
        distManner = templateObj.getDistMannerAt(templateIndex);
        distMannerString = templateObj.getDistMannerStringAt(templateIndex);
      }

      templateIndex++;
    }

    if(templateIndexArg == null) {
      throw new XMPexception("cannot find index '" + loopIndexName + "' reference in <on-ref>");
    }

    if(templateIndex != templateDim) {
      throw new XMPexception("wrong template dimensions, too few");
    }

    Ident parallelInitId = declIdentWithBlock(schedBaseBlock,
                                              "_XMP_loop_init_" + loopIndexName, Xtype.intType);
    Ident parallelCondId = declIdentWithBlock(schedBaseBlock,
                                              "_XMP_loop_cond_" + loopIndexName, Xtype.intType);
    Ident parallelStepId = declIdentWithBlock(schedBaseBlock,
                                              "_XMP_loop_step_" + loopIndexName, Xtype.intType);

    XMPutil.putLoopIter(schedBaseBlock, loopIndexName,
                        Xcons.List(parallelInitId, parallelCondId, parallelStepId));

    switch (distManner) {
      case XMPtemplate.DUPLICATION:
      case XMPtemplate.BLOCK:
      case XMPtemplate.CYCLIC:
      case XMPtemplate.BLOCK_CYCLIC:
        forBlock.setLowerBound(parallelInitId.Ref());
        forBlock.setUpperBound(parallelCondId.Ref());
        forBlock.setStep(parallelStepId.Ref());
        break;
      default:
        throw new XMPexception("unknown distribute manner");
    }

    // rewrite loop index in loop
    BasicBlockExprIterator iter = new BasicBlockExprIterator(getLoopBody(forBlock));

    for (iter.init(); !iter.end(); iter.next()) {
      XMPrewriteExpr.rewriteLoopIndexInLoop(iter.getExpr(), loopIndexName,
                                            templateObj, templateIndexArg.getInt(),
                                            _globalDecl, XMPlocalDecl.getXMPsymbolTable(forBlock));
    }
  }

  private void callLoopSchedFuncNodes(XMPnodes nodesObj, XobjList nodesSubscriptList,
                                      CforBlock forBlock, CforBlock schedBaseBlock) throws XMPexception {
    Xobject loopIndex = forBlock.getInductionVar();
    Xtype loopIndexType = loopIndex.Type();

    if (!XMPutil.isIntegerType(loopIndexType)) {
      throw new XMPexception("loop index variable has a non-integer type");
    }

    String loopIndexName = loopIndex.getSym();

    int nodesIndex = 0;
    int nodesDim = nodesObj.getDim();
    XobjInt nodesIndexArg = null;
    for (XobjArgs i = nodesSubscriptList.getArgs(); i != null; i = i.nextArgs()) {
      if (nodesIndex >= nodesDim) {
        throw new XMPexception("wrong nodes dimensions, too many");
      }

      String s = i.getArg().getString();
      if (s.equals(loopIndexName)) {
        if (nodesIndexArg != null) {
          throw new XMPexception("loop index '" + loopIndexName + "' is already described");
        }

        nodesIndexArg = Xcons.IntConstant(nodesIndex);
      }

      nodesIndex++;
    }

    if (nodesIndexArg == null) {
      throw new XMPexception("cannot find index '" + loopIndexName + "' reference in <on-ref>");
    }

    if (nodesIndex != nodesDim) {
      throw new XMPexception("wrong nodes dimensions, too few");
    }

    Ident parallelInitId = declIdentWithBlock(schedBaseBlock,
                                              "_XMP_loop_init_" + loopIndexName, loopIndexType);
    Ident parallelCondId = declIdentWithBlock(schedBaseBlock,
                                              "_XMP_loop_cond_" + loopIndexName, loopIndexType);
    Ident parallelStepId = declIdentWithBlock(schedBaseBlock,
                                              "_XMP_loop_step_" + loopIndexName, loopIndexType);

    XMPutil.putLoopIter(schedBaseBlock, loopIndexName,
                        Xcons.List(parallelInitId, parallelCondId, parallelStepId));

    forBlock.setLowerBound(parallelInitId.Ref());
    forBlock.setUpperBound(parallelCondId.Ref());
    forBlock.setStep(parallelStepId.Ref());

    forBlock.getCondBBlock().setExpr(Xcons.binaryOp(Xcode.LOG_LT_EXPR, loopIndex, parallelCondId.Ref()));
  }

  private Ident declIdentWithBlock(Block b, String identName, Xtype type) {
    BlockList bl = b.getParent();

    // FIXME consider variable scope
    return bl.declLocalIdent(identName, type);
  }

  private Block createCommTaskBlock(BlockList body, String execFuncSurfix, XobjList execFuncArgs) throws XMPexception {
    // setup barrier finalizer
    setupFinalizer(body, _globalDecl.declExternFunc("_XMP_pop_nodes"), null);

    // create function call
    BlockList taskBody = Bcons.emptyBody();
    Ident taskDescId = Ident.Local("_XMP_TASK_desc", Xtype.voidPtrType);
    execFuncArgs.cons(taskDescId.getAddr());
    Ident execFuncId = _globalDecl.declExternFunc("_XMP_exec_task_" + execFuncSurfix, Xtype.intType);
    Block execBlock = Bcons.IF(BasicBlock.Cond(execFuncId.Call(execFuncArgs)), body, null);
    taskBody.add(execBlock);

    return Bcons.COMPOUND(taskBody);
  }

  private void translateBarrier(PragmaBlock pb) throws XMPexception {
    // start translation
    XobjList barrierDecl = (XobjList)pb.getClauses();
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);

    // create function call
    Block barrierFuncCallBlock = null;
    XobjList onRef = (XobjList)barrierDecl.getArg(0);
    if (onRef == null) {
      barrierFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_barrier_EXEC", null);
    } else {
      XMPquadruplet<String, Boolean, XobjList, XMPobject> execOnRefArgs = createExecOnRefArgs(onRef, localXMPsymbolTable);
      String execFuncSurfix = execOnRefArgs.getFirst();
      boolean splitComm = execOnRefArgs.getSecond().booleanValue();
      XobjList execFuncArgs = execOnRefArgs.getThird();
      if (splitComm) {
        BlockList barrierBody = Bcons.blockList(_globalDecl.createFuncCallBlock("_XMP_barrier_EXEC", null));
	barrierFuncCallBlock = createCommTaskBlock(barrierBody, execFuncSurfix, execFuncArgs);
      } else {
	barrierFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_barrier_" + execFuncSurfix, execFuncArgs);
      }
    }

    pb.replace(barrierFuncCallBlock);

    // add function calls for profiling                                                                                
    Xobject profileClause = barrierDecl.getArg(1);
    if ( _all_profile || (profileClause != null && _selective_profile)){
	if (doScalasca == true) {
	    XobjList profileFuncArgs = Xcons.List(Xcons.StringConstant("#xmp barrier:" + pb.getLineNo()));
	    barrierFuncCallBlock.insert(createScalascaStartProfileCall(profileFuncArgs));
	    barrierFuncCallBlock.add(createScalascaEndProfileCall(profileFuncArgs));
	} else if (doTlog == true) {
	    barrierFuncCallBlock.insert(
					createTlogMacroInvoke("_XMP_M_TLOG_BARRIER_IN", null));
	    barrierFuncCallBlock.add(
				     createTlogMacroInvoke("_XMP_M_TLOG_BARRIER_OUT", null));
	}
    } else if (profileClause == null && _selective_profile && doTlog == false){
	XobjList profileFuncArgs = null;
	barrierFuncCallBlock.insert(createScalascaProfileOffCall(profileFuncArgs));
	barrierFuncCallBlock.add(createScalascaProfileOnfCall(profileFuncArgs));
    }

  }

  private void translateReduction(PragmaBlock pb) throws XMPexception {
    // start translation
    XobjList reductionDecl = (XobjList)pb.getClauses();
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);

    // create function arguments
    XobjList reductionRef = (XobjList)reductionDecl.getArg(0);
    Vector<XobjList> reductionFuncArgsList = createReductionArgsList(reductionRef, pb,
                                                                     false, null, null);
    String reductionFuncType = createReductionFuncType(reductionRef, pb);

    // create function call
    Block reductionFuncCallBlock = null;
    XobjList onRef = (XobjList)reductionDecl.getArg(1);
    if (onRef == null) {
	reductionFuncCallBlock = createReductionFuncCallBlock(true, reductionFuncType + "_EXEC", null, reductionFuncArgsList);
    }
    else {
      XMPquadruplet<String, Boolean, XobjList, XMPobject> execOnRefArgs = createExecOnRefArgs(onRef, localXMPsymbolTable);
      String execFuncSurfix = execOnRefArgs.getFirst();
      boolean splitComm = execOnRefArgs.getSecond().booleanValue();
      XobjList execFuncArgs = execOnRefArgs.getThird();
      if (splitComm) {
        BlockList reductionBody = Bcons.blockList(createReductionFuncCallBlock(true, reductionFuncType + "_EXEC",
                                                                               null, reductionFuncArgsList));
	reductionFuncCallBlock = createCommTaskBlock(reductionBody, execFuncSurfix, execFuncArgs);
      }
      else {
        reductionFuncCallBlock = createReductionFuncCallBlock(false, reductionFuncType + "_" + execFuncSurfix,
                                                              execFuncArgs.operand(), reductionFuncArgsList);
      }
    }
    
    pb.replace(reductionFuncCallBlock);

    // add function calls for profiling                                                                                    
    Xobject profileClause = reductionDecl.getArg(2);
    if( _all_profile || (profileClause != null && _selective_profile)){
        if (doScalasca == true) {
            XobjList profileFuncArgs = Xcons.List(Xcons.StringConstant("#xmp reduction:" + pb.getLineNo()));
            reductionFuncCallBlock.insert(createScalascaStartProfileCall(profileFuncArgs));
            reductionFuncCallBlock.add(createScalascaEndProfileCall(profileFuncArgs));
        } else if (doTlog == true) {
            reductionFuncCallBlock.insert(
					  createTlogMacroInvoke("_XMP_M_TLOG_REDUCTION_IN", null));
            reductionFuncCallBlock.add(
				       createTlogMacroInvoke("_XMP_M_TLOG_REDUCTION_OUT", null));
        }
    } else if(profileClause == null && _selective_profile && doTlog == false){
        XobjList profileFuncArgs = null;
        reductionFuncCallBlock.insert(createScalascaProfileOffCall(profileFuncArgs));
        reductionFuncCallBlock.add(createScalascaProfileOnfCall(profileFuncArgs));
    }
  }

  private String createReductionFuncType(XobjList reductionRef, PragmaBlock pb) throws XMPexception {
    XobjInt reductionOp = (XobjInt)reductionRef.getArg(0);
    switch (reductionOp.getInt()) {
      case XMPcollective.REDUCE_SUM:
      case XMPcollective.REDUCE_PROD:
      case XMPcollective.REDUCE_BAND:
      case XMPcollective.REDUCE_LAND:
      case XMPcollective.REDUCE_BOR:
      case XMPcollective.REDUCE_LOR:
      case XMPcollective.REDUCE_BXOR:
      case XMPcollective.REDUCE_LXOR:
      case XMPcollective.REDUCE_MAX:
      case XMPcollective.REDUCE_MIN:
        return new String("reduce");
      case XMPcollective.REDUCE_FIRSTMAX:
      case XMPcollective.REDUCE_FIRSTMIN:
      case XMPcollective.REDUCE_LASTMAX:
      case XMPcollective.REDUCE_LASTMIN:
        return new String("reduce_FLMM");
      default:
        throw new XMPexception("unknown reduce operation");
    }
  }

  private Vector<XobjList> createReductionArgsList(XobjList reductionRef, PragmaBlock pb, boolean isClause,
                                                   CforBlock schedBaseBlock, IfBlock reductionInitIfBlock) throws XMPexception {
    Vector<XobjList> returnVector = new Vector<XobjList>();

    XobjInt reductionOp = (XobjInt)reductionRef.getArg(0);
    XobjList reductionSpecList = (XobjList)reductionRef.getArg(1);
    for (XobjArgs i = reductionSpecList.getArgs(); i != null; i = i.nextArgs()) {
      XobjList reductionSpec = (XobjList)i.getArg();
      String specName = reductionSpec.getArg(0).getString();

      XMPpair<Ident, Xtype> typedSpec = XMPutil.findTypedVar(specName, pb);
      Ident specId = typedSpec.getFirst();
      Xtype specType = typedSpec.getSecond();

      boolean isArray = false;
      boolean isPointer = false;
      Xobject specRef = null;
      Xobject count = null;
      Xobject elmtType = null;
      BasicType basicSpecType = null;
      switch (specType.getKind()) {
      case Xtype.BASIC:
	{
	  basicSpecType = (BasicType)specType;
	  checkReductionType(specName, basicSpecType);
	  
	  specRef = specId.getAddr();
	  count = Xcons.LongLongConstant(0, 1);
	  elmtType = XMP.createBasicTypeConstantObj(basicSpecType);
	} break;
      case Xtype.ARRAY:
	{
	  isArray = true;
	  ArrayType arraySpecType = (ArrayType)specType;
	  if (arraySpecType.getArrayElementType().getKind() != Xtype.BASIC)
	    throw new XMPexception("array '" + specName + "' has a wrong data type for reduction");
	  
	  basicSpecType = (BasicType)arraySpecType.getArrayElementType();
	  checkReductionType(specName, basicSpecType);
	  
	  // FIXME not good implementation
	  XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);
	  XMPalignedArray specAlignedArray = _globalDecl.getXMPalignedArray(specName, localXMPsymbolTable);
	  if (specAlignedArray == null) {
	    specRef = specId.Ref();
	    count = Xcons.LongLongConstant(0, XMPutil.getArrayElmtCount(arraySpecType));
	  }
	  else {
	    if (isClause) {
	      throw new XMPexception("aligned arrays cannot be used in reduction clause");
	    }
	    
	    if (specAlignedArray.hasShadow()) {
	      throw new XMPexception("arrays which have shadow cannot be used in reduction directive/clause");
	    }
	    
	    specRef = specAlignedArray.getAddrIdVoidRef();
	    Ident getTotalElmtFuncId = _globalDecl.declExternFunc("_XMP_get_array_total_elmts",
								  Xtype.unsignedlonglongType);
	    count = getTotalElmtFuncId.Call(Xcons.List(specAlignedArray.getDescId().Ref()));
	  }

	  elmtType = XMP.createBasicTypeConstantObj(basicSpecType);
	} break;
        case Xtype.POINTER:
        {
          isPointer = true;
          basicSpecType = (BasicType)specType.getRef();
          specRef = specId.getAddr();
          count = Xcons.LongLongConstant(0, 1);
          elmtType = XMP.createBasicTypeConstantObj(basicSpecType);
        } break;
      default:
	throw new XMPexception("'" + specName + "' has a wrong data type for reduction");
      }

      XobjList reductionFuncArgs = Xcons.List(specRef, count, elmtType, reductionOp);

      // declare temp variable for reduction
      if (isClause) {
        createReductionInitStatement(specId, isArray, count, basicSpecType, reductionOp.getInt(),
                                     schedBaseBlock, reductionInitIfBlock);
      }
      
      if(isPointer){
	Xobject varaddr = (Xobject)reductionFuncArgs.getArg(0);
	reductionFuncArgs.setArg(0, Xcons.PointerRef(varaddr));
      }

      // add extra args for (firstmax, firstmin, lastmax, lastmin) if needed
      createFLMMreductionArgs(reductionOp.getInt(), (XobjList)reductionSpec.getArg(1), reductionFuncArgs, pb);

      returnVector.add(reductionFuncArgs);
    }

    return returnVector;
  }

  private void createReductionInitStatement(Ident varId, boolean isArray, Xobject count, BasicType type, int reductionOp,
                                            CforBlock schedBaseBlock, IfBlock reductionInitIfBlock) throws XMPexception {
    if (!needsInitialization(reductionOp)) {
      return;
    }

    BlockList initPart = reductionInitIfBlock.getThenBody();
    if (initPart == null) {
      initPart = Bcons.emptyBody();
      reductionInitIfBlock.setThenBody(initPart);
    }

    Xobject statement = null;
    if (isArray) {
      Ident initLoopIndexId = declIdentWithBlock(schedBaseBlock, _globalDecl.genSym(XMP.TEMP_PREFIX), Xtype.unsignedlonglongType);
      initPart.add(createReductionArrayInit(varId, createReductionInitValueObj(varId, type, reductionOp), count, type, initLoopIndexId));
    }
    else {
      initPart.add(Xcons.Set(varId.Ref(), createReductionInitValueObj(varId, type, reductionOp)));
    }
  }

  private boolean needsInitialization(int reductionOp) throws XMPexception {
    switch (reductionOp) {
      case XMPcollective.REDUCE_SUM:
      case XMPcollective.REDUCE_PROD:
      case XMPcollective.REDUCE_BXOR:
      case XMPcollective.REDUCE_LXOR:
        return true;
      case XMPcollective.REDUCE_BAND:
      case XMPcollective.REDUCE_LAND:
      case XMPcollective.REDUCE_BOR:
      case XMPcollective.REDUCE_LOR:
      case XMPcollective.REDUCE_MAX:
      case XMPcollective.REDUCE_MIN:
      case XMPcollective.REDUCE_FIRSTMAX:
      case XMPcollective.REDUCE_FIRSTMIN:
      case XMPcollective.REDUCE_LASTMAX:
      case XMPcollective.REDUCE_LASTMIN:
        return false;
      default:
        throw new XMPexception("unknown reduce operation");
    }
  }

  private Block createReductionArrayInit(Ident tempId, Xobject initValueObj, Xobject count, BasicType type,
                                         Ident loopIndexId) {
    Xobject loopIndexRef = loopIndexId.Ref();

    Xobject tempArrayRef = Xcons.PointerRef(Xcons.binaryOp(Xcode.PLUS_EXPR, Xcons.Cast(Xtype.Pointer(type), tempId.Ref()),
							   loopIndexRef));

    Block loopBlock = Bcons.FOR(Xcons.Set(loopIndexRef, Xcons.IntConstant(0)),
                                Xcons.binaryOp(Xcode.LOG_LT_EXPR, loopIndexRef, count),
                                Xcons.asgOp(Xcode.ASG_PLUS_EXPR, loopIndexRef, Xcons.IntConstant(1)),
                                Bcons.Statement(Xcons.Set(tempArrayRef, initValueObj)));

    return loopBlock;
  }

  // FIXME
  private Xobject createReductionInitValueObj(Ident varId, BasicType type, int reductionOp) throws XMPexception {
    Xobject varRef = varId.Ref();
    Xobject intZero = Xcons.IntConstant(0);
    Xobject intOne = Xcons.IntConstant(1);
    Xobject floatZero = Xcons.FloatConstant(0.);
    Xobject floatOne = Xcons.FloatConstant(1.);

    switch (type.getBasicType()) {
      case BasicType.BOOL:
        // FIXME correct???
        return selectReductionInitValueObj(reductionOp, varRef, intZero, intOne);
      case BasicType.CHAR:
      case BasicType.UNSIGNED_CHAR:
      case BasicType.SHORT:
      case BasicType.UNSIGNED_SHORT:
      case BasicType.INT:
      case BasicType.UNSIGNED_INT:
      case BasicType.LONG:
      case BasicType.UNSIGNED_LONG:
      case BasicType.LONGLONG:
      case BasicType.UNSIGNED_LONGLONG:
        return selectReductionInitValueObj(reductionOp, varRef, intZero, intOne);
      case BasicType.FLOAT:
      case BasicType.DOUBLE:
      case BasicType.LONG_DOUBLE:
        return selectReductionInitValueObj(reductionOp, varRef, floatZero, floatOne);
      case BasicType.FLOAT_IMAGINARY:
      case BasicType.DOUBLE_IMAGINARY:
      case BasicType.LONG_DOUBLE_IMAGINARY:
      case BasicType.FLOAT_COMPLEX:
      case BasicType.DOUBLE_COMPLEX:
      case BasicType.LONG_DOUBLE_COMPLEX:
        // FIXME not implemented yet
        throw new XMPexception("not implemented yet");
      default:
        throw new XMPexception("wrong data type for reduction");
    }
  }

  // FIXME needs type checking
  private Xobject selectReductionInitValueObj(int reductionOp, Xobject varRef, Xobject zero, Xobject one) throws XMPexception {
    switch (reductionOp) {
      case XMPcollective.REDUCE_SUM:
        return zero;
      case XMPcollective.REDUCE_PROD:
        return one;
      case XMPcollective.REDUCE_BAND:
      case XMPcollective.REDUCE_LAND:
      case XMPcollective.REDUCE_BOR:
      case XMPcollective.REDUCE_LOR:
      case XMPcollective.REDUCE_MAX:
      case XMPcollective.REDUCE_MIN:
      case XMPcollective.REDUCE_FIRSTMAX:
      case XMPcollective.REDUCE_FIRSTMIN:
      case XMPcollective.REDUCE_LASTMAX:
      case XMPcollective.REDUCE_LASTMIN:
        throw new XMPexception("the operation does not need initialization");
      case XMPcollective.REDUCE_BXOR:
        return Xcons.unaryOp(Xcode.BIT_NOT_EXPR, varRef);
      case XMPcollective.REDUCE_LXOR:
        return Xcons.unaryOp(Xcode.LOG_NOT_EXPR, varRef);
      default:
        throw new XMPexception("unknown reduce operation");
    }
  }

  private void createFLMMreductionArgs(int op, XobjList locationVars, XobjList funcArgs, PragmaBlock pb) throws XMPexception {
    switch (op) {
      case XMPcollective.REDUCE_SUM:
      case XMPcollective.REDUCE_PROD:
      case XMPcollective.REDUCE_BAND:
      case XMPcollective.REDUCE_LAND:
      case XMPcollective.REDUCE_BOR:
      case XMPcollective.REDUCE_LOR:
      case XMPcollective.REDUCE_BXOR:
      case XMPcollective.REDUCE_LXOR:
      case XMPcollective.REDUCE_MAX:
      case XMPcollective.REDUCE_MIN:
        return;
      case XMPcollective.REDUCE_FIRSTMAX:
      case XMPcollective.REDUCE_FIRSTMIN:
      case XMPcollective.REDUCE_LASTMAX:
      case XMPcollective.REDUCE_LASTMIN:
        break;
      default:
        throw new XMPexception("unknown reduce operation");
    }

    funcArgs.add(Xcons.IntConstant(XMPutil.countElmts(locationVars)));

    // check <location-variables> and add to funcArgs
    for (XobjArgs i = locationVars.getArgs(); i != null; i = i.nextArgs()) {
      String varName = i.getArg().getString();

      XMPpair<Ident, Xtype> typedVar = XMPutil.findTypedVar(varName, pb);
      Ident varId = typedVar.getFirst();
      Xtype varType = typedVar.getSecond();

      switch (varType.getKind()) {
        case Xtype.BASIC:
          {
	    //            if (!XMPutil.isIntegerType(varType))
	    //              throw new XMPexception("'" + varName + "' should have a integer type for reduction");

            BasicType basicVarType = (BasicType)varType;

            funcArgs.add(Xcons.Cast(Xtype.voidPtrType, varId.getAddr()));
            funcArgs.add(Xcons.Cast(Xtype.intType, XMP.createBasicTypeConstantObj(basicVarType)));
          } break;
        case Xtype.ARRAY:
          {
            ArrayType arrayVarType = (ArrayType)varType;
            if (arrayVarType.getArrayElementType().getKind() != Xtype.BASIC)
              throw new XMPexception("array '" + varName + "' has has a wrong data type for reduction");

            BasicType basicVarType = (BasicType)arrayVarType.getArrayElementType();

            if (!XMPutil.isIntegerType(basicVarType))
              throw new XMPexception("'" + varName + "' should have a integer type for reduction");

            funcArgs.add(Xcons.Cast(Xtype.voidPtrType, varId.Ref()));
            funcArgs.add(Xcons.Cast(Xtype.intType, XMP.createBasicTypeConstantObj(basicVarType)));
          } break;
        case Xtype.POINTER:
  	  {
	    PointerType ptrVarType = (PointerType)varType;
	    BasicType basicVarType = (BasicType)ptrVarType.getRef();
	    funcArgs.add(Xcons.Cast(Xtype.voidPtrType, varId.Ref()));
	    funcArgs.add(Xcons.Cast(Xtype.intType, XMP.createBasicTypeConstantObj(basicVarType)));
	  } break;
        default:
          throw new XMPexception("'" + varName + "' has a wrong data type for reduction");
      }
    }
  }

  private void checkReductionType(String name, BasicType type) throws XMPexception {
    switch (type.getBasicType()) {
      case BasicType.BOOL:
      case BasicType.CHAR:
      case BasicType.UNSIGNED_CHAR:
      case BasicType.SHORT:
      case BasicType.UNSIGNED_SHORT:
      case BasicType.INT:
      case BasicType.UNSIGNED_INT:
      case BasicType.LONG:
      case BasicType.UNSIGNED_LONG:
      case BasicType.LONGLONG:
      case BasicType.UNSIGNED_LONGLONG:
      case BasicType.FLOAT:
      case BasicType.DOUBLE:
      case BasicType.LONG_DOUBLE:
      case BasicType.FLOAT_IMAGINARY:
      case BasicType.DOUBLE_IMAGINARY:
      case BasicType.LONG_DOUBLE_IMAGINARY:
      case BasicType.FLOAT_COMPLEX:
      case BasicType.DOUBLE_COMPLEX:
      case BasicType.LONG_DOUBLE_COMPLEX:
        break;
      default:
        throw new XMPexception("'" + name + "' has a wrong data type for reduction");
    }
  }

  private Block createReductionFuncCallBlock(boolean isMacroFunc, String funcType,
                                             Xobject execDesc, Vector<XobjList> funcArgsList) {
    Ident funcId = null;
    if (isMacroFunc) funcId = XMP.getMacroId("_XMP_M_" + funcType.toUpperCase());
    else             funcId = _globalDecl.declExternFunc("_XMP_" + funcType);

    BlockList funcCallList = Bcons.emptyBody();
    Iterator<XobjList> it = funcArgsList.iterator();
    while (it.hasNext()) {
      XobjList funcArgs = it.next();
      if (execDesc != null) funcArgs.cons(execDesc);

      funcCallList.add(Bcons.Statement(funcId.Call(funcArgs)));
    }
    return Bcons.COMPOUND(funcCallList);
  }

  private void translateBcast(PragmaBlock pb) throws XMPexception {
    // start translation
    XobjList bcastDecl = (XobjList)pb.getClauses();
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);

    // create function arguments
    XobjList varList = (XobjList)bcastDecl.getArg(0);
    Vector<XobjList> bcastArgsList = createBcastArgsList(varList, pb);

    // create function call
    Block bcastFuncCallBlock = null;
    XobjList fromRef = (XobjList)bcastDecl.getArg(1);
    XMPpair<String, XobjList> execFromRefArgs = null;
    if (fromRef != null) execFromRefArgs = createExecFromRefArgs(fromRef, localXMPsymbolTable);

    XobjList onRef = (XobjList)bcastDecl.getArg(2);
    if (onRef == null) {
	bcastFuncCallBlock = createBcastFuncCallBlock(true, "EXEC", null, bcastArgsList, execFromRefArgs);
    } else {
      XMPquadruplet<String, Boolean, XobjList, XMPobject> execOnRefArgs = createExecOnRefArgs(onRef, localXMPsymbolTable);

      String execFuncSurfix = execOnRefArgs.getFirst();
      boolean splitComm = execOnRefArgs.getSecond().booleanValue();
      XobjList execFuncArgs = execOnRefArgs.getThird();
      if (splitComm) {
        BlockList bcastBody = Bcons.blockList(createBcastFuncCallBlock(true, "EXEC",
                                                                       null, bcastArgsList, execFromRefArgs));
	bcastFuncCallBlock = createCommTaskBlock(bcastBody, execFuncSurfix, execFuncArgs);
      }
      else {
	bcastFuncCallBlock = createBcastFuncCallBlock(false, execFuncSurfix,
                                            execFuncArgs.operand(), bcastArgsList, execFromRefArgs);
      }
    }

    pb.replace(bcastFuncCallBlock);

    // add function calls for profiling                                                                                    
    Xobject profileClause = bcastDecl.getArg(3);
    if( _all_profile || (profileClause != null && _selective_profile)){
        if (doScalasca == true) {
            XobjList profileFuncArgs = Xcons.List(Xcons.StringConstant("#xmp bcast:" + pb.getLineNo()));
            bcastFuncCallBlock.insert(createScalascaStartProfileCall(profileFuncArgs));
            bcastFuncCallBlock.add(createScalascaEndProfileCall(profileFuncArgs));
        } else if (doTlog == true) {
            bcastFuncCallBlock.insert(
				      createTlogMacroInvoke("_XMP_M_TLOG_BCAST_IN", null));
            bcastFuncCallBlock.add(
				   createTlogMacroInvoke("_XMP_M_TLOG_BCAST_OUT", null));
        }
    } else if(profileClause == null && _selective_profile && doTlog == false){
        XobjList profileFuncArgs = null;
        bcastFuncCallBlock.insert(createScalascaProfileOffCall(profileFuncArgs));
        bcastFuncCallBlock.add(createScalascaProfileOnfCall(profileFuncArgs));
    }
  }

  private Vector<XobjList> createBcastArgsList(XobjList varList, PragmaBlock pb) throws XMPexception {
    Vector<XobjList> returnVector = new Vector<XobjList>();

    for (XobjArgs i = varList.getArgs(); i != null; i = i.nextArgs()) {
      String varName = i.getArg().getString();

      XMPpair<Ident, Xtype> typedSpec = XMPutil.findTypedVar(varName, pb);
      Ident varId = typedSpec.getFirst();
      Xtype varType = typedSpec.getSecond();

      XobjLong count = null;
      switch (varType.getKind()) {
        case Xtype.BASIC:
        case Xtype.STRUCT:
        case Xtype.UNION:
          {
            count = Xcons.LongLongConstant(0, 1);
            returnVector.add(Xcons.List(varId.getAddr(), count, Xcons.SizeOf(varType)));
          } break;
        case Xtype.POINTER:
	  {
	    count = Xcons.LongLongConstant(0, 1);
	    returnVector.add(Xcons.List(varId.Ref(), count, Xcons.SizeOf(varType)));
  	  } break;
        case Xtype.ARRAY:
          {
            ArrayType arrayVarType = (ArrayType)varType;
            switch (arrayVarType.getArrayElementType().getKind()) {
              case Xtype.BASIC:
              case Xtype.STRUCT:
              case Xtype.UNION:
                break;
              default:
                throw new XMPexception("array '" + varName + "' has has a wrong data type for broadcast");
            }

            count = Xcons.LongLongConstant(0, XMPutil.getArrayElmtCount(arrayVarType));
            returnVector.add(Xcons.List(varId.Ref(), count, Xcons.SizeOf(((ArrayType)varType).getArrayElementType())));
          } break;
        default:
          throw new XMPexception("'" + varName + "' has a wrong data type for broadcast");
      }
    }

    return returnVector;
  }

  private Block createBcastFuncCallBlock(boolean isMacro, String funcType, Xobject execDesc, Vector<XobjList> funcArgsList,
                                         XMPpair<String, XobjList> execFromRefArgs) throws XMPexception {
    String funcSurfix = null;
    XobjList fromRef = null;
    if (execFromRefArgs == null) funcSurfix = new String(funcType + "_OMITTED");
    else {
      funcSurfix = new String(funcType + "_" + execFromRefArgs.getFirst());
      fromRef = execFromRefArgs.getSecond();
    }

    Ident funcId = null;
    if (isMacro) funcId = XMP.getMacroId("_XMP_M_BCAST_" + funcSurfix);
    else         funcId = _globalDecl.declExternFunc("_XMP_bcast_" + funcSurfix);

    BlockList funcCallList = Bcons.emptyBody();
    Iterator<XobjList> it = funcArgsList.iterator();
    while (it.hasNext()) {
      XobjList funcArgs = it.next();
      if (execDesc != null) funcArgs.cons(execDesc);
      if (execFromRefArgs != null) funcArgs.mergeList(fromRef);

      funcCallList.add(Bcons.Statement(funcId.Call(funcArgs)));
    }
    return Bcons.COMPOUND(funcCallList);
  }

  private XMPpair<String, XobjList> createExecFromRefArgs(XobjList fromRef,
                                                          XMPsymbolTable localXMPsymbolTable) throws XMPexception {
    if (fromRef.getArg(0) == null) {
      // execute on global communicator
      XobjList globalRef = (XobjList)fromRef.getArg(1);

      XobjList execFuncArgs = Xcons.List();
      // lower
      if (globalRef.getArg(0) == null)
        throw new XMPexception("lower bound cannot be omitted in <from-ref>");
      else execFuncArgs.add(globalRef.getArg(0));

      // upper
      if (globalRef.getArg(1) == null)
        throw new XMPexception("upper bound cannot be omitted in <from-ref>");
      else execFuncArgs.add(globalRef.getArg(1));

      // stride
      if (globalRef.getArg(2) == null) execFuncArgs.add(Xcons.IntConstant(1));
      else execFuncArgs.add(globalRef.getArg(2));

      return new XMPpair<String, XobjList>(new String("GLOBAL"), execFuncArgs);
    }
    else {
      // execute on <object-ref>

      // check object name collision
      String objectName = fromRef.getArg(0).getString();
      XMPobject fromRefObject = _globalDecl.getXMPobject(objectName, localXMPsymbolTable);
      if (fromRefObject == null) {
        throw new XMPexception("cannot find '" + objectName + "' nodes/template");
      }

      if (fromRefObject.getKind() == XMPobject.TEMPLATE)
        throw new XMPexception("template cannot be used in <from-ref>");

      // create arguments
      if (fromRef.getArg(1) == null)
        throw new XMPexception("multiple source nodes indicated in bcast directive");
      else {
        XobjList execFuncArgs = Xcons.List(fromRefObject.getDescId().Ref());

        int refIndex = 0;
        int refDim = fromRefObject.getDim();
        for (XobjArgs i = fromRef.getArg(1).getArgs(); i != null; i = i.nextArgs()) {
          if (refIndex == refDim)
            throw new XMPexception("wrong nodes dimension indicated, too many");

          XobjList t = (XobjList)i.getArg();
          if (t == null) execFuncArgs.add(Xcons.Cast(Xtype.intType, Xcons.IntConstant(1)));
          else {
            execFuncArgs.add(Xcons.Cast(Xtype.intType, Xcons.IntConstant(0)));

            // lower
            if (t.getArg(0) == null)
              throw new XMPexception("lower bound cannot be omitted in <from-ref>");
            else execFuncArgs.add(Xcons.Cast(Xtype.intType, t.getArg(0)));

            // upper
            if (t.getArg(1) == null)
              throw new XMPexception("upper bound cannot be omitted in <from-ref>");
            else execFuncArgs.add(Xcons.Cast(Xtype.intType, t.getArg(1)));

            // stride
            if (t.getArg(2) == null) execFuncArgs.add(Xcons.Cast(Xtype.intType, Xcons.IntConstant(1)));
            else execFuncArgs.add(Xcons.Cast(Xtype.intType, t.getArg(2)));
          }

          refIndex++;
        }

        if (refIndex != refDim)
          throw new XMPexception("the number of <nodes/template-subscript> should be the same with the dimension");

        return new XMPpair<String, XobjList>(new String("NODES"), execFuncArgs);
      }
    }
  }

  private void translateGmove(PragmaBlock pb) throws XMPexception {
    // start translation
    XobjList gmoveDecl = (XobjList)pb.getClauses();
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);
    BlockList gmoveBody = pb.getBody();
    Block gmoveFuncCallBlock = null;

    // gmove in/out is not implemented
    Xobject gmoveClause = gmoveDecl.getArg(0);
    if(XMPcollective.GMOVE_IN == gmoveClause.getInt() || XMPcollective.GMOVE_OUT == gmoveClause.getInt())
      throw new XMPexception("gmove in/out directive is not supported yet");

    // check body
    Xobject assignStmt = null;
    String checkBodyErrMsg = new String("gmove directive should be written before one assign statement");
    Block gmoveBodyHead = gmoveBody.getHead();
    if(gmoveBodyHead instanceof SimpleBlock) {
      if (gmoveBodyHead.getNext() != null) {
        throw new XMPexception(checkBodyErrMsg);
      }

      Statement gmoveStmt = gmoveBodyHead.getBasicBlock().getHead();
      if (gmoveStmt.getNext() != null) {
        throw new XMPexception(checkBodyErrMsg);
      }

      if(gmoveStmt.getExpr().Opcode() == Xcode.ASSIGN_EXPR) {
        assignStmt = gmoveStmt.getExpr();
      } else {
        throw new XMPexception(checkBodyErrMsg);
      }
    } else {
      throw new XMPexception(checkBodyErrMsg);
    }

    // FIXME consider in, out clause
    Xobject leftExpr = assignStmt.left();
    XMPpair<XMPalignedArray, XobjList> leftExprInfo = getXMPalignedArrayExpr(pb, leftExpr);
    XMPalignedArray leftAlignedArray = leftExprInfo.getFirst();

    Xobject rightExpr = assignStmt.right();
    XMPpair<XMPalignedArray, XobjList> rightExprInfo = getXMPalignedArrayExpr(pb, assignStmt.right());
    XMPalignedArray rightAlignedArray = rightExprInfo.getFirst();

    boolean leftHasSubArrayRef = (leftExpr.Opcode() == Xcode.SUB_ARRAY_REF);
    boolean rightHasSubArrayRef = (rightExpr.Opcode() == Xcode.SUB_ARRAY_REF);
    if (leftHasSubArrayRef) {
      if (rightHasSubArrayRef) {
        if (leftAlignedArray == null) {
          if (rightAlignedArray == null) {	// !leftIsAlignedArray && !rightIsAlignedArray  |-> local assignment (every node)
            String arrayName = getArrayName(leftExpr);
            Ident arrayId = pb.findVarIdent(arrayName);
            Xtype arrayElmtType = arrayId.Type().getArrayElementType();

            XobjList gmoveFuncArgs = null;
            if (arrayElmtType.getKind() == Xtype.BASIC) {
              gmoveFuncArgs = Xcons.List(XMP.createBasicTypeConstantObj(arrayElmtType));
            } else {
              gmoveFuncArgs = Xcons.List(Xcons.IntConstant(XMP.NONBASIC_TYPE));
            }
            gmoveFuncArgs.add(Xcons.SizeOf(arrayElmtType));

            gmoveFuncArgs.mergeList(leftExprInfo.getSecond());
            gmoveFuncArgs.mergeList(rightExprInfo.getSecond());
	    gmoveFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_gmove_LOCALCOPY_ARRAY", gmoveFuncArgs);
          } else {				// !leftIsAlignedArray &&  rightIsAlignedArray  |-> broadcast
            Xtype arrayElmtType = rightAlignedArray.getType();

            XobjList gmoveFuncArgs = Xcons.List(rightAlignedArray.getDescId().Ref());
            if (arrayElmtType.getKind() == Xtype.BASIC) {
              gmoveFuncArgs.add(XMP.createBasicTypeConstantObj(arrayElmtType));
            } else {
              gmoveFuncArgs.add(Xcons.IntConstant(XMP.NONBASIC_TYPE));
            }
            gmoveFuncArgs.add(Xcons.SizeOf(arrayElmtType));

            gmoveFuncArgs.mergeList(leftExprInfo.getSecond());
            gmoveFuncArgs.mergeList(rightExprInfo.getSecond());
	    gmoveFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_gmove_BCAST_ARRAY", gmoveFuncArgs);
          }
        } else {
          if (rightAlignedArray == null) {	//  leftIsAlignedArray && !rightIsAlignedArray  |-> local assignment (home node)
            Xtype arrayElmtType = leftAlignedArray.getType();

            XobjList gmoveFuncArgs = Xcons.List(leftAlignedArray.getDescId().Ref());
            if (arrayElmtType.getKind() == Xtype.BASIC) {
              gmoveFuncArgs.add(XMP.createBasicTypeConstantObj(arrayElmtType));
            } else {
              gmoveFuncArgs.add(Xcons.IntConstant(XMP.NONBASIC_TYPE));
            }
            gmoveFuncArgs.add(Xcons.SizeOf(arrayElmtType));

            gmoveFuncArgs.mergeList(leftExprInfo.getSecond());
            gmoveFuncArgs.mergeList(rightExprInfo.getSecond());
	    gmoveFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_gmove_HOMECOPY_ARRAY", gmoveFuncArgs);
          } else {				//  leftIsAlignedArray &&  rightIsAlignedArray  |-> send/recv
            Xtype arrayElmtType = leftAlignedArray.getType();

            XobjList gmoveFuncArgs = Xcons.List(leftAlignedArray.getDescId().Ref(),
                                                rightAlignedArray.getDescId().Ref());
            if (arrayElmtType.getKind() == Xtype.BASIC) {
              gmoveFuncArgs.add(XMP.createBasicTypeConstantObj(arrayElmtType));
            } else {
              gmoveFuncArgs.add(Xcons.IntConstant(XMP.NONBASIC_TYPE));
            }
            gmoveFuncArgs.add(Xcons.SizeOf(arrayElmtType));

            gmoveFuncArgs.mergeList(leftExprInfo.getSecond());
            gmoveFuncArgs.mergeList(rightExprInfo.getSecond());

	    boolean flag = false;
	    for(int i=0;i<leftAlignedArray.getDim();i++){   // 1
	      if(leftAlignedArray.getAlignMannerAt(i) == XMPalignedArray.NOT_ALIGNED){
		flag = true;
	      }
	    }

	    for(int i=0;i<rightAlignedArray.getDim();i++){  // 2
	      if(rightAlignedArray.getAlignMannerAt(i) == XMPalignedArray.NOT_ALIGNED){
		flag = false;
	      }
	    }

	    String leftAlignedArrayTemplate  = leftAlignedArray.getAlignTemplate().getName();
	    String rightAlignedArrayTemplate = rightAlignedArray.getAlignTemplate().getName();
	    if(!leftAlignedArrayTemplate.equals(rightAlignedArrayTemplate)){ // 3
	      flag = false;
	    }
	    if(!(leftAlignedArray.getDim() == 2 && rightAlignedArray.getDim() == 2)){  // 4
	      flag = false;
	    }

	    if(flag == true){
	      // 1. One of dimension of left array is not aligned.
	      // 2. All dimensions of right array are aligned (temporary).
	      // 3. Templates of left array amd right array are the same.
	      // 4. Both left array amd right array are must 2 dimentional array (temporary).
	      gmoveFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_gmove_BCAST_TO_NOTALIGNED_ARRAY", gmoveFuncArgs);
	    }
	    else{
	      gmoveFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_gmove_SENDRECV_ARRAY", gmoveFuncArgs);
	    }
          }
        }
      } else {
        // FIXME implement
        throw new XMPexception("not implemented yet");
      }
    } else {
      if (rightHasSubArrayRef) {
        throw new XMPexception("syntax error in gmove assign statement");
      }

      if (leftAlignedArray == null) {
        if (rightAlignedArray == null) {	// !leftIsAlignedArray && !rightIsAlignedArray	|-> local assignment (every node)
	  gmoveFuncCallBlock = Bcons.COMPOUND(gmoveBody);
        } else {				// !leftIsAlignedArray &&  rightIsAlignedArray	|-> broadcast
          XobjList gmoveFuncArgs = Xcons.List(Xcons.AddrOf(leftExpr), Xcons.AddrOf(rightExpr),
                                              rightAlignedArray.getDescId().Ref());
          gmoveFuncArgs.mergeList(rightExprInfo.getSecond());

	  gmoveFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_gmove_BCAST_SCALAR", gmoveFuncArgs);
        }
      } else {
        if (rightAlignedArray == null) {	//  leftIsAlignedArray && !rightIsAlignedArray	|-> local assignment (home node)
          XobjList gmoveFuncArgs = Xcons.List(leftAlignedArray.getDescId().Ref());
          gmoveFuncArgs.mergeList(leftExprInfo.getSecond());

          Ident gmoveFuncId = _globalDecl.declExternFunc("_XMP_gmove_HOMECOPY_SCALAR", Xtype.intType);
	  gmoveFuncCallBlock = Bcons.IF(BasicBlock.Cond(gmoveFuncId.Call(gmoveFuncArgs)), gmoveBody, null);
        } else {				//  leftIsAlignedArray &&  rightIsAlignedArray	|-> send/recv
          XobjList gmoveFuncArgs = Xcons.List(Xcons.AddrOf(leftExpr), Xcons.AddrOf(rightExpr),
                                              leftAlignedArray.getDescId().Ref(), rightAlignedArray.getDescId().Ref());
          gmoveFuncArgs.mergeList(leftExprInfo.getSecond());
          gmoveFuncArgs.mergeList(rightExprInfo.getSecond());

	  gmoveFuncCallBlock = _globalDecl.createFuncCallBlock("_XMP_gmove_SENDRECV_SCALAR", gmoveFuncArgs);
        }
      }
    }
    
    // Why is the barrier needed ?
    //Block gmoveBlock = Bcons.COMPOUND(Bcons.blockList(gmoveFuncCallBlock, _globalDecl.createFuncCallBlock("_XMP_barrier_EXEC", null)));
    Block gmoveBlock = Bcons.COMPOUND(Bcons.blockList(gmoveFuncCallBlock));
    pb.replace(gmoveBlock);

    // add function calls for profiling                                                                                    
    Xobject profileClause = gmoveDecl.getArg(1);
    if( _all_profile || (profileClause != null && _selective_profile)){
        if (doScalasca == true) {
            XobjList profileFuncArgs = Xcons.List(Xcons.StringConstant("#xmp gmove:" + pb.getLineNo()));
            gmoveBlock.insert(createScalascaStartProfileCall(profileFuncArgs));
            gmoveBlock.add(createScalascaEndProfileCall(profileFuncArgs));
        } else if (doTlog == true) {
            gmoveBlock.insert(createTlogMacroInvoke("_XMP_M_TLOG_GMOVE_IN", null));
            gmoveBlock.add(createTlogMacroInvoke("_XMP_M_TLOG_GMOVE_OUT", null));
        }
    } else if(profileClause == null && _selective_profile && doTlog == false){
        XobjList profileFuncArgs = null;
        gmoveBlock.insert(createScalascaProfileOffCall(profileFuncArgs));
        gmoveBlock.add(createScalascaProfileOnfCall(profileFuncArgs));
    }
    
  }

  private XMPpair<XMPalignedArray, XobjList> getXMPalignedArrayExpr(PragmaBlock pb, Xobject expr) throws XMPexception {
    switch (expr.Opcode()) {
      case ARRAY_REF:
        return parseArrayRefExpr(pb, expr);
      case SUB_ARRAY_REF:
        return parseSubArrayRefExpr(pb, expr, getArrayAccList(pb, expr));
      default:
        // FIXME support var-ref
        throw new XMPexception("unsupported expression: gmove");
    }
  }

  private String getArrayName(Xobject expr) throws XMPexception {
    if ((expr.Opcode() == Xcode.ARRAY_REF) ||
        (expr.Opcode() == Xcode.SUB_ARRAY_REF)) {
      return expr.getArg(0).getSym();
    } else {
      throw new XMPexception("cannot find array ref");
    }
  }

  private XobjList getArrayAccList(PragmaBlock pb, Xobject expr) throws XMPexception {
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);

    XobjList accList = Xcons.List();

    String arrayName = expr.getArg(0).getSym();
    XMPalignedArray alignedArray = _globalDecl.getXMPalignedArray(arrayName, localXMPsymbolTable);
    if (alignedArray == null) {
      Ident arrayId = pb.findVarIdent(arrayName);
      Xtype arrayType = arrayId.Type();

      int arrayDim = arrayType.getNumDimensions();
      if (arrayDim > XMP.MAX_DIM) {
        throw new XMPexception("array dimension should be less than " + (XMP.MAX_DIM + 1));
      }

      arrayType = arrayType.getRef();
      for (int i = 0; i < arrayDim - 1; i++, arrayType = arrayType.getRef()) {
        accList.add(XMPutil.getArrayElmtsObj(arrayType));
      }
      accList.add(Xcons.IntConstant(1));
    } else {
      int arrayDim = alignedArray.getDim();
      for (int i = 0; i < arrayDim; i++) {
        accList.add(alignedArray.getAccIdAt(i).Ref());
      }
    }

    return accList;
  }

  private XMPpair<XMPalignedArray, XobjList> parseSubArrayRefExpr(PragmaBlock pb,
                                                                  Xobject expr, XobjList accList) throws XMPexception {
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);
    Xobject arrayAddr = expr.getArg(0);
    String arrayName = arrayAddr.getSym();
    XMPalignedArray alignedArray = _globalDecl.getXMPalignedArray(arrayName, localXMPsymbolTable);

    XobjList arrayRefs = (XobjList)expr.getArg(1);
    XobjList castedArrayRefs = Xcons.List();

    int dim = 0;
    if (arrayRefs != null) {
      for (Xobject x : arrayRefs) {
        if (x.Opcode() == Xcode.LIST) {
          castedArrayRefs.add(Xcons.Cast(Xtype.intType, x.getArg(0)));
          castedArrayRefs.add(Xcons.Cast(Xtype.intType, x.getArg(1)));
          castedArrayRefs.add(Xcons.Cast(Xtype.intType, x.getArg(2)));
          castedArrayRefs.add(Xcons.Cast(Xtype.unsignedlonglongType, accList.getArg(dim)));
        } else {
          castedArrayRefs.add(Xcons.Cast(Xtype.intType, x));
          castedArrayRefs.add(Xcons.Cast(Xtype.intType, x));
          castedArrayRefs.add(Xcons.Cast(Xtype.intType, Xcons.IntConstant(1)));
          castedArrayRefs.add(Xcons.Cast(Xtype.unsignedlonglongType, accList.getArg(dim)));
        }

       dim++;
      }
    }

    if (alignedArray == null) {
      castedArrayRefs.cons(Xcons.Cast(Xtype.intType, Xcons.IntConstant(dim)));
      castedArrayRefs.cons(Xcons.Cast(Xtype.voidPtrType, arrayAddr));
    }

    return new XMPpair<XMPalignedArray, XobjList>(alignedArray, castedArrayRefs);
  }

  private XMPpair<XMPalignedArray, XobjList> parseArrayRefExpr(PragmaBlock pb, Xobject expr) throws XMPexception {
    XMPsymbolTable localXMPsymbolTable = XMPlocalDecl.declXMPsymbolTable(pb);
    String arrayName = expr.getArg(0).getSym();

    XMPalignedArray alignedArray = _globalDecl.getXMPalignedArray(arrayName, localXMPsymbolTable);
    XobjList castedArrayRefs = Xcons.List();
    XobjList arrayRefs = (XobjList)expr.getArg(1);
    if (arrayRefs != null) {
      for (Xobject x : arrayRefs) {
        castedArrayRefs.add(Xcons.Cast(Xtype.intType, x));
      }
    }

    return new XMPpair<XMPalignedArray, XobjList>(alignedArray, castedArrayRefs);
  }

  private XMPquadruplet<String, Boolean, XobjList, XMPobject> createExecOnRefArgs(XobjList onRef,
                                                                                  XMPsymbolTable localXMPsymbolTable) throws XMPexception {
    if (onRef.getArg(0) == null) {
      // execute on global communicator
      XobjList globalRef = (XobjList)onRef.getArg(1);

      boolean splitComm = false;
      XobjList tempArgs = Xcons.List();
      // lower
      if (globalRef.getArg(0) == null) tempArgs.add(Xcons.IntConstant(1));
      else {
        splitComm = true;
        tempArgs.add(globalRef.getArg(0));
      }
      // upper
      if (globalRef.getArg(1) == null) tempArgs.add(_globalDecl.getWorldSizeId().Ref());
      else {
        splitComm = true;
        tempArgs.add(globalRef.getArg(1));
      }
      // stride
      if (globalRef.getArg(2) == null) tempArgs.add(Xcons.IntConstant(1));
      else {
        splitComm = true;
        tempArgs.add(globalRef.getArg(2));
      }

      String execFuncSurfix = null;
      XobjList execFuncArgs = null;
      if (splitComm) {
        execFuncSurfix = "GLOBAL_PART";
        execFuncArgs = tempArgs;
      }
      else {
        execFuncSurfix = "NODES_ENTIRE";
        execFuncArgs = Xcons.List(_globalDecl.getWorldDescId().Ref());
      }

      return new XMPquadruplet<String, Boolean, XobjList, XMPobject>(execFuncSurfix, new Boolean(splitComm), execFuncArgs, null);
    }
    else {
      // execute on <object-ref>

      // check object name collision
      String objectName = onRef.getArg(0).getString();
      XMPobject onRefObject = _globalDecl.getXMPobject(objectName, localXMPsymbolTable);
      if (onRefObject == null) {
        throw new XMPexception("cannot find '" + objectName + "' nodes/template");
      }

      Xobject ontoNodesRef = null;
      Xtype castType = null;
      switch (onRefObject.getKind()) {
        case XMPobject.NODES:
          ontoNodesRef = onRefObject.getDescId().Ref();
          castType = Xtype.intType;
          break;
        case XMPobject.TEMPLATE:
          {
            XMPtemplate ontoTemplate = (XMPtemplate)onRefObject;

            if (!ontoTemplate.isFixed()) {
              throw new XMPexception("template '" + objectName + "' is not fixed");
            }

            if (!ontoTemplate.isDistributed()) {
              throw new XMPexception("template '" + objectName + "' is not distributed");
            }

            XMPnodes ontoNodes = ((XMPtemplate)onRefObject).getOntoNodes();

            ontoNodesRef = ontoNodes.getDescId().Ref();
            castType = Xtype.longlongType;
            break;
          }
        default:
          throw new XMPexception("unknown object type");
      }

      // create arguments
      if (onRef.getArg(1) == null)
        return new XMPquadruplet<String, Boolean, XobjList, XMPobject>(new String("NODES_ENTIRE"), new Boolean(false), Xcons.List(ontoNodesRef), onRefObject);
      else {
        boolean splitComm = false;
        int refIndex = 0;
        int refDim = onRefObject.getDim();
        XobjList tempArgs = Xcons.List();
        for (XobjArgs i = onRef.getArg(1).getArgs(); i != null; i = i.nextArgs()) {
          if (refIndex == refDim)
            throw new XMPexception("wrong nodes dimension indicated, too many");

          XobjList t = (XobjList)i.getArg();
          if (t == null) {
            splitComm = true;
            tempArgs.add(Xcons.Cast(Xtype.intType, Xcons.IntConstant(1)));
          }
          else {
            tempArgs.add(Xcons.Cast(Xtype.intType, Xcons.IntConstant(0)));

            // lower
            if (t.getArg(0) == null) {
              tempArgs.add(Xcons.Cast(castType, onRefObject.getLowerAt(refIndex)));
            } else {
              splitComm = true;
              tempArgs.add(Xcons.Cast(castType, t.getArg(0)));
            }
            // upper
            if (t.getArg(1) == null) {
              tempArgs.add(Xcons.Cast(castType, onRefObject.getUpperAt(refIndex)));
            }
            else {
              splitComm = true;
              tempArgs.add(Xcons.Cast(castType, t.getArg(1)));
            }
            // stride
            if (t.getArg(2) == null) tempArgs.add(Xcons.Cast(castType, Xcons.IntConstant(1)));
            else {
              splitComm = true;
              // XXX stride: always int
              tempArgs.add(Xcons.Cast(castType, t.getArg(2)));
            }
          }

          refIndex++;
        }

        if (refIndex != refDim)
          throw new XMPexception("the number of <nodes/template-subscript> should be the same with the dimension");

        if (splitComm) {
          String execFuncSurfix = null;
          XobjList execFuncArgs = null;
          execFuncArgs = tempArgs;
          switch (onRefObject.getKind()) {
            case XMPobject.NODES:
              execFuncSurfix = "NODES_PART";
              execFuncArgs.cons(ontoNodesRef);
              break;
            case XMPobject.TEMPLATE:
              execFuncSurfix = "TEMPLATE_PART";
              execFuncArgs.cons(((XMPtemplate)onRefObject).getDescId().Ref());
              break;
            default:
              throw new XMPexception("unknown object type");
          }

          return new XMPquadruplet<String, Boolean, XobjList, XMPobject>(execFuncSurfix, new Boolean(splitComm), execFuncArgs, onRefObject);
        }
        else
          return new XMPquadruplet<String, Boolean, XobjList, XMPobject>(new String("NODES_ENTIRE"),
                                                              new Boolean(splitComm), Xcons.List(ontoNodesRef),
                                                              onRefObject);
      }
    }
  }

  private void setupFinalizer(BlockList body, Ident funcId, XobjList args) throws XMPexception {
    BlockIterator i = new topdownBlockIterator(body);
    for (i.init(); !i.end(); i.next()) {
      Block b = i.getBlock();
      if (b.Opcode() == Xcode.GOTO_STATEMENT)
        throw new XMPexception("cannot use goto statement here");
      else if (b.Opcode() == Xcode.RETURN_STATEMENT)
        b.insert(funcId.Call(args));
    }
    body.add(funcId.Call(args));
  }

  public static void checkDeclPragmaLocation(PragmaBlock pb) throws XMPexception {
/*
    String pragmaNameL = pb.getPragma().toLowerCase();
    String errMsg = new String(pragmaNameL +
                               " directive should be written before declarations, statements and executable directives");

    // check parent
    Block parentBlock = pb.getParentBlock();
    if (parentBlock.Opcode() != Xcode.COMPOUND_STATEMENT)
      throw new XMPexception(errMsg);
    else {
      BlockList parent = pb.getParent();
      Xobject declList = parent.getDecls();
      if (declList != null) {
        if (declList.operand() != null)
          throw new XMPexception(errMsg);
      }

      if (parentBlock.getParentBlock().Opcode() != Xcode.FUNCTION_DEFINITION)
        throw new XMPexception(errMsg);
    }

    // check previous blocks
    for (Block prevBlock = pb.getPrev(); prevBlock != null; prevBlock = prevBlock.getPrev()) {
      if (prevBlock.Opcode() == Xcode.XMP_PRAGMA) {
        XMPpragma prevPragma = XMPpragma.valueOf(((PragmaBlock)prevBlock).getPragma());
        switch (XMPpragma.valueOf(((PragmaBlock)prevBlock).getPragma())) {
          case NODES:
          case TEMPLATE:
          case DISTRIBUTE:
          case ALIGN:
          case SHADOW:
            continue;
          default:
            throw new XMPexception(errMsg);
        }
      }
      else
        throw new XMPexception(errMsg);
    }
*/
    // XXX delete this
    return;
  }

  public void set_all_profile(){
      _all_profile = true;
  }

  public void set_selective_profile(){
      _selective_profile = true;
  }

  public void setScalascaEnabled(boolean v) {
      doScalasca = v;
  }

  public void setTlogEnabled(boolean v) {
      doTlog = v;
  }

    private Block createScalascaStartProfileCall(XobjList funcArgs) {
        Ident funcId = XMP.getMacroId("_XMP_M_EPIK_USER_START");
        BlockList funcCallList = Bcons.emptyBody();

        funcCallList.add(Bcons.Statement(funcId.Call(funcArgs)));
        return Bcons.COMPOUND(funcCallList);
    }

    private Block createScalascaEndProfileCall(XobjList funcArgs) {
        Ident funcId = XMP.getMacroId("_XMP_M_EPIK_USER_END");
        BlockList funcCallList = Bcons.emptyBody();

        funcCallList.add(Bcons.Statement(funcId.Call(funcArgs)));
        return Bcons.COMPOUND(funcCallList);
    }

    private Block createScalascaProfileOffCall(XobjList funcArgs) {
        Ident funcId = XMP.getMacroId("_XMP_M_EPIK_GEN_OFF");
        BlockList funcCallList = Bcons.emptyBody();

        funcCallList.add(Bcons.Statement(funcId.Call(funcArgs)));
        return Bcons.COMPOUND(funcCallList);
    }

    private Block createScalascaProfileOnfCall(XobjList funcArgs) {
        Ident funcId = XMP.getMacroId("_XMP_M_EPIK_GEN_ON");
        BlockList funcCallList = Bcons.emptyBody();

        funcCallList.add(Bcons.Statement(funcId.Call(funcArgs)));
        return Bcons.COMPOUND(funcCallList);
    }

    private Block createTlogMacroInvoke(String macro, XobjList funcArgs) {
        Ident macroId = XMP.getMacroId(macro);
        BlockList funcCallList = Bcons.emptyBody();
        funcCallList.add(Bcons.Statement(macroId.Call(funcArgs)));
        return Bcons.COMPOUND(funcCallList);
    }
}
